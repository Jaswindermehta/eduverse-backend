package com.eduverse.service;

import com.eduverse.dto.CourseContentDto;
import com.eduverse.dto.CourseCreateRequest;
import com.eduverse.dto.CourseResponse;
import com.eduverse.dto.CourseUpdateRequest;
import com.eduverse.entity.*;
import com.eduverse.exception.ResourceNotFoundException;
import com.eduverse.mapper.CourseMapper;
import com.eduverse.repository.CategoryRepository;
import com.eduverse.repository.CourseRepository;
import com.eduverse.repository.ReviewRepository;
import com.eduverse.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * COURSE SERVICE
 * ============================================================================
 * 
 * Orchestrates business logic, soft deletion, paginated dynamic queries, and
 * identity ownership checks for the entire course catalog.
 * 
 * CORE FEATURES:
 * - **Instructor Ownership Validation**: Only course authors can edit/delete.
 * - **Soft Delete**: Toggles course visibility active = false.
 * - **N+1 Optimization Integration**: Relies on optimized repositories and mappers.
 * - **ACID Compliance**: Enforced via @Transactional markers.
 */
@Service
public class CourseService {

    private static final Logger logger = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final CourseMapper courseMapper;

    // Strict constructor-based dependency injection
    public CourseService(
            CourseRepository courseRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            ReviewRepository reviewRepository,
            CourseMapper courseMapper
    ) {
        this.courseRepository = courseRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.courseMapper = courseMapper;
    }

    /**
     * Creates a new Course. Only INSTRUCTOR role is authorized.
     */
    @CacheEvict(value = {"courses", "courseDetails"}, allEntries = true)
    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request) {
        logger.info("Attempting to create course: {}", request.getTitle());

        // 1. Fetch the category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        // 2. Fetch the currently authenticated instructor from Spring SecurityContext
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User instructor = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // 3. Construct and map the Course entity
        Course course = new Course();
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setPrice(request.getPrice());
        course.setCategory(category);
        course.setInstructor(instructor);
        course.setActive(true); // default active state

        // 4. Map and append course modules/chapters (establishing bi-directional links)
        if (request.getContents() != null) {
            for (CourseContentDto contentDto : request.getContents()) {
                CourseContent content = new CourseContent();
                content.setTitle(contentDto.getTitle());
                content.setContentUrl(contentDto.getContentUrl());
                content.setSequenceOrder(contentDto.getSequenceOrder());
                course.addContent(content); // bi-directional sync helper
            }
        }

        Course savedCourse = courseRepository.save(course);
        logger.info("Course successfully created with ID: {}", savedCourse.getId());
        
        // Since it's a new course, rating statistics default to 0.0 / 0
        return courseMapper.toCourseResponse(savedCourse, 0.0, 0);
    }

    /**
     * Updates an existing Course. Enforces ownership boundary.
     */
    @CacheEvict(value = {"courses", "courseDetails"}, allEntries = true)
    @Transactional
    public CourseResponse updateCourse(Long id, CourseUpdateRequest request) {
        logger.info("Attempting to update course with ID: {}", id);

        // 1. Fetch the course
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));

        // 2. Verify ownership: Only the course's instructor or Admin can update
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        verifyOwnership(course, currentUser);

        // 3. Fetch new category if changed
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        // 4. Bind changes
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setPrice(request.getPrice());
        course.setCategory(category);

        // Update Course Lectures by completely refreshing contents list (JPA orphanRemoval handles db sync)
        course.getContents().clear();
        if (request.getContents() != null) {
            for (CourseContentDto contentDto : request.getContents()) {
                CourseContent content = new CourseContent();
                content.setTitle(contentDto.getTitle());
                content.setContentUrl(contentDto.getContentUrl());
                content.setSequenceOrder(contentDto.getSequenceOrder());
                course.addContent(content);
            }
        }

        Course updatedCourse = courseRepository.save(course);
        logger.info("Course with ID: {} successfully updated", id);

        // Calculate and attach review stats
        Double avgRating = reviewRepository.getAverageRatingByCourseId(id);
        Integer totalReviews = reviewRepository.countReviewsByCourseId(id);

        return courseMapper.toCourseResponse(updatedCourse, avgRating, totalReviews);
    }

    /**
     * Soft-deletes a course. Toggles course visibility active = false.
     * Enforces ownership validation.
     */
    @CacheEvict(value = {"courses", "courseDetails"}, allEntries = true)
    @Transactional
    public void deleteCourse(Long id) {
        logger.info("Attempting soft-delete on course with ID: {}", id);

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));

        // Verify ownership
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        verifyOwnership(course, currentUser);

        // Perform Soft Delete
        course.setActive(false);
        courseRepository.save(course);
        logger.info("Course with ID: {} was successfully soft-deleted (active = false)", id);
    }

    /**
     * Gets a single active course by ID (loads full contents and statistics).
     */
    @Cacheable(value = "courseDetails", key = "#id")
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        logger.debug("Fetching course details for ID: {}", id);

        Course course = courseRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));

        Double avgRating = reviewRepository.getAverageRatingByCourseId(id);
        Integer totalReviews = reviewRepository.countReviewsByCourseId(id);

        return courseMapper.toCourseResponse(course, avgRating, totalReviews);
    }

    /**
     * Performs dynamic, paginated filtering and searching on active courses.
     */
    @Cacheable(value = "courses", key = "{#title, #categoryId, #instructorId, #maxPrice, #pageable.pageNumber, #pageable.pageSize, #pageable.sort.toString()}")
    @Transactional(readOnly = true)
    public Page<CourseResponse> searchCourses(
            String title,
            Long categoryId,
            Long instructorId,
            BigDecimal maxPrice,
            Pageable pageable
    ) {
        logger.debug("Filtering course catalog dynamically...");

        // Fetch paginated active entities (pre-fetched via JOIN FETCH in repository)
        Page<Course> coursePage = courseRepository.searchCourses(title, categoryId, instructorId, maxPrice, pageable);

        // Map to CourseResponses, attaching pre-computed rating stats for each course in the page
        return coursePage.map(course -> {
            Double avgRating = reviewRepository.getAverageRatingByCourseId(course.getId());
            Integer totalReviews = reviewRepository.countReviewsByCourseId(course.getId());
            return courseMapper.toCourseResponse(course, avgRating, totalReviews);
        });
    }

    /**
     * Validates if the current user is authorized to modify a course.
     * Admin accounts bypass ownership checks automatically.
     */
    private void verifyOwnership(Course course, User currentUser) {
        // If user is ADMIN, they are granted immediate clearance
        if (currentUser.getRole().getRoleName().equals(RoleName.ADMIN)) {
            return;
        }

        // If user is NOT the instructor who owns the course, reject
        if (!course.getInstructor().getId().equals(currentUser.getId())) {
            logger.warn("User '{}' denied permission to modify course ID: {}", currentUser.getEmail(), course.getId());
            throw new AccessDeniedException("Access Denied: You do not have permission to modify this course.");
        }
    }
}

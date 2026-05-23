package com.eduverse.service;

import com.eduverse.dto.ReviewCreateRequest;
import com.eduverse.dto.ReviewResponse;
import com.eduverse.entity.Course;
import com.eduverse.entity.Review;
import com.eduverse.entity.User;
import com.eduverse.exception.ResourceNotFoundException;
import com.eduverse.mapper.CourseMapper;
import com.eduverse.repository.CourseRepository;
import com.eduverse.repository.EnrollmentRepository;
import com.eduverse.repository.ReviewRepository;
import com.eduverse.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * REVIEW SERVICE
 * ============================================================================
 * 
 * Orchestrates business operations for posting and reading course reviews.
 * 
 * CRITICAL BUSINESS RULES:
 * - **Review Eligibility Check**: A student can ONLY review a course if they are
 *   actively enrolled in it!
 * - Constructor Injection (SOLID).
 * - Centralized @Transactional markers.
 */
@Service
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseMapper courseMapper;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    // Constructor Injection
    public ReviewService(
            ReviewRepository reviewRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository,
            CourseMapper courseMapper,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.reviewRepository = reviewRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseMapper = courseMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Posts a new course review.
     * Enforces the critical rule that the student must be enrolled in the course.
     */
    @Transactional
    public ReviewResponse addReview(Long courseId, ReviewCreateRequest request) {
        logger.info("Attempting to post review for course ID: {}", courseId);

        // 1. Fetch the course, ensuring it is active
        Course course = courseRepository.findByIdAndActiveTrue(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        // 2. Fetch currently authenticated student from Spring Security
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // 3. Enforce the critical rule: STUDENT can review only enrolled courses!
        boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId());
        if (!isEnrolled) {
            logger.warn("Student '{}' review posting rejected: not enrolled in course ID {}", student.getEmail(), courseId);
            throw new IllegalArgumentException("You can only review courses you are enrolled in.");
        }

        // 4. Construct and save the Review
        Review review = new Review();
        review.setStudent(student);
        review.setCourse(course);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review savedReview = reviewRepository.save(review);
        logger.info("Student '{}' successfully reviewed course ID {} with a {}-star rating", 
                student.getEmail(), courseId, request.getRating());

        // Publish event for loose-coupled asynchronous dispatches (email + DB alerts)
        eventPublisher.publishEvent(new com.eduverse.event.ReviewCreatedEvent(this, savedReview));

        return courseMapper.toReviewResponse(savedReview);
    }

    /**
     * Fetches a paginated, sorted list of all reviews submitted for a specific course.
     * Uses optimized JPQL FETCH JOIN query to prevent student details N+1 lookups.
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByCourseId(Long courseId, Pageable pageable) {
        logger.debug("Fetching reviews list for course ID: {}", courseId);

        if (!courseRepository.existsByIdAndActiveTrue(courseId)) {
            throw new ResourceNotFoundException("Course", "id", courseId);
        }

        Page<Review> reviews = reviewRepository.findByCourseIdWithStudent(courseId, pageable);
        return reviews.map(courseMapper::toReviewResponse);
    }
}

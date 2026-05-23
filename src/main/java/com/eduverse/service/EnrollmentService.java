package com.eduverse.service;

import com.eduverse.dto.EnrollmentResponse;
import com.eduverse.entity.Course;
import com.eduverse.entity.Enrollment;
import com.eduverse.entity.User;
import com.eduverse.exception.ResourceNotFoundException;
import com.eduverse.mapper.CourseMapper;
import com.eduverse.repository.CourseRepository;
import com.eduverse.repository.EnrollmentRepository;
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
 * ENROLLMENT SERVICE
 * ============================================================================
 * 
 * Manages the registration lifecycle of students enrolling in online courses.
 * 
 * DESIGN BOUNDARIES:
 * - Constructor Injection (SOLID).
 * - **Duplicate Protection**: Validates if a student is already registered.
 * - **SecurityContext retrieval**: Automatically binds the authenticated caller.
 */
@Service
public class EnrollmentService {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentService.class);

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseMapper courseMapper;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    // Constructor Injection
    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseMapper courseMapper,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.courseMapper = courseMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Enrolls the currently authenticated student in a specific course.
     * Marks with @Transactional to ensure ACID database operations.
     */
    @Transactional
    public EnrollmentResponse enrollInCourse(Long courseId) {
        logger.info("Attempting student enrollment for course ID: {}", courseId);

        // 1. Fetch the course, ensuring it exists and is active
        Course course = courseRepository.findByIdAndActiveTrue(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        // 2. Fetch currently authenticated student from Spring Security
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // 3. Prevent duplicate enrollment
        boolean alreadyEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId());
        if (alreadyEnrolled) {
            logger.warn("Student '{}' registration failed: already registered for course ID {}", student.getEmail(), courseId);
            throw new IllegalArgumentException("You are already enrolled in this course.");
        }

        // 4. Create and persist the Enrollment record
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        logger.info("Student '{}' successfully enrolled in course '{}' (Enrollment ID: {})", 
                student.getEmail(), course.getTitle(), savedEnrollment.getId());

        // Publish event for loose-coupled asynchronous dispatches (email + DB alerts)
        eventPublisher.publishEvent(new com.eduverse.event.EnrollmentCreatedEvent(this, savedEnrollment));

        return courseMapper.toEnrollmentResponse(savedEnrollment);
    }

    /**
     * Retrieves all course enrollments for the currently authenticated student.
     * Uses optimized JPQL FETCH JOIN query to prevent N+1 queries.
     */
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getMyEnrollments(Pageable pageable) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        logger.debug("Fetching enrollment logs for student: {}", student.getEmail());
        Page<Enrollment> enrollments = enrollmentRepository.findByStudentIdWithCourse(student.getId(), pageable);
        
        return enrollments.map(courseMapper::toEnrollmentResponse);
    }
}

package com.eduverse.controller;

import com.eduverse.dto.*;
import com.eduverse.service.CourseService;
import com.eduverse.service.EnrollmentService;
import com.eduverse.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * ============================================================================
 * COURSE CONTROLLER
 * ============================================================================
 * 
 * Exposes RESTful gateways for catalog operations, enrollments, and review postings.
 */
@RestController
@RequestMapping("/api/courses")
@Tag(name = "Course Management", description = "Endpoints for course publishing, searching, enrollments, and reviews")
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final ReviewService reviewService;

    // Strict constructor-based injection
    public CourseController(
            CourseService courseService,
            EnrollmentService enrollmentService,
            ReviewService reviewService
    ) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.reviewService = reviewService;
    }

    /**
     * Publishes a new course (Instructor Only).
     */
    @PostMapping
    @Operation(summary = "Create a new course (Instructor Only)", description = "Publishes a new online course catalog. Locked to INSTRUCTOR role.")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(@Valid @RequestBody CourseCreateRequest request) {
        CourseResponse created = courseService.createCourse(request);
        ApiResponse<CourseResponse> response = new ApiResponse<>(
                true,
                "Course successfully published.",
                created
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing course (Instructor Owner Only).
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a course (Instructor Owner Only)", description = "Updates details and lecture modules of an existing course. Only the publishing instructor can update.")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseUpdateRequest request
    ) {
        CourseResponse updated = courseService.updateCourse(id, request);
        ApiResponse<CourseResponse> response = new ApiResponse<>(
                true,
                "Course successfully updated.",
                updated
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Soft-deletes a course (Instructor Owner Only / Admin).
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a course", description = "Performs a safe soft-delete operation, hiding it from searching but maintaining records in database.")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        ApiResponse<Void> response = new ApiResponse<>(
                true,
                "Course successfully archived.",
                null
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Public course detail lookup by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get course by ID", description = "Publicly retrieves active course details, associated chapters, and computed rating metrics.")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseById(@PathVariable Long id) {
        CourseResponse course = courseService.getCourseById(id);
        ApiResponse<CourseResponse> response = new ApiResponse<>(
                true,
                "Course details retrieved successfully.",
                course
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Public, dynamic, paginated search and filter endpoint.
     */
    @GetMapping
    @Operation(summary = "List and search courses dynamically", description = "Publicly lists active courses using optional query parameters for pagination, sorting, and specific filters.")
    public ResponseEntity<ApiResponse<Page<CourseResponse>>> searchCourses(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long instructorId,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        // Construct standard Pageable with Sorting
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CourseResponse> courses = courseService.searchCourses(title, categoryId, instructorId, maxPrice, pageable);
        ApiResponse<Page<CourseResponse>> response = new ApiResponse<>(
                true,
                "Course catalog list retrieved successfully.",
                courses
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Enrolls the currently authenticated student in a course (Student Only).
     */
    @PostMapping("/{id}/enroll")
    @Operation(summary = "Enroll in a course (Student Only)", description = "Enrolls the authenticated student in the specified course. Restricts duplicate registrations.")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enrollInCourse(@PathVariable Long id) {
        EnrollmentResponse enrollment = enrollmentService.enrollInCourse(id);
        ApiResponse<EnrollmentResponse> response = new ApiResponse<>(
                true,
                "Enrollment successful! Access granted.",
                enrollment
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Adds a review for a course (Student Enrolled Only).
     */
    @PostMapping("/{id}/reviews")
    @Operation(summary = "Add a review (Student Enrolled Only)", description = "Registers feedback rating (1-5 stars) and review text. Restricted only to enrolled students.")
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        ReviewResponse review = reviewService.addReview(id, request);
        ApiResponse<ReviewResponse> response = new ApiResponse<>(
                true,
                "Review successfully posted. Thank you for your feedback!",
                review
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Public paginated lookup of course reviews.
     */
    @GetMapping("/{id}/reviews")
    @Operation(summary = "Fetch reviews for a course", description = "Publicly retrieves paginated reviews left for a specific course.")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getCourseReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewResponse> reviews = reviewService.getReviewsByCourseId(id, pageable);
        
        ApiResponse<Page<ReviewResponse>> response = new ApiResponse<>(
                true,
                "Course reviews list retrieved successfully.",
                reviews
        );
        return ResponseEntity.ok(response);
    }
}

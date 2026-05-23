package com.eduverse.repository;

import com.eduverse.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * ============================================================================
 * ENROLLMENT REPOSITORY
 * ============================================================================
 * 
 * Interacts directly with the 'enrollments' database schema.
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // Validates if a student has already registered for a course
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    // Counts total student enrollments for a course (used to display stats)
    long countByCourseId(Long courseId);

    /**
     * Fetches a paginated list of enrollments for a student.
     * Prevents N+1 by pre-fetching the Course metadata in a single query.
     */
    @Query(value = "SELECT e FROM Enrollment e " +
                   "JOIN FETCH e.course " +
                   "WHERE e.student.id = :studentId",
           countQuery = "SELECT count(e) FROM Enrollment e WHERE e.student.id = :studentId")
    Page<Enrollment> findByStudentIdWithCourse(@Param("studentId") Long studentId, Pageable pageable);
}

package com.eduverse.repository;

import com.eduverse.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * ============================================================================
 * REVIEW REPOSITORY
 * ============================================================================
 * 
 * Interacts directly with the 'reviews' database schema.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Paginated lookup of all reviews left for a specific course
    @Query(value = "SELECT r FROM Review r " +
                   "JOIN FETCH r.student " +
                   "WHERE r.course.id = :courseId",
           countQuery = "SELECT count(r) FROM Review r WHERE r.course.id = :courseId")
    Page<Review> findByCourseIdWithStudent(@Param("courseId") Long courseId, Pageable pageable);

    /**
     * Calculates the average rating of a course directly via database aggregation.
     * Extremely efficient compared to pulling all records into memory.
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.course.id = :courseId")
    Double getAverageRatingByCourseId(@Param("courseId") Long courseId);

    /**
     * Counts the total number of reviews for a course.
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.course.id = :courseId")
    Integer countReviewsByCourseId(@Param("courseId") Long courseId);
}

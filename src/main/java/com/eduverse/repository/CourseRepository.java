package com.eduverse.repository;

import com.eduverse.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * ============================================================================
 * COURSE REPOSITORY
 * ============================================================================
 * 
 * Interacts directly with the 'courses' database schema.
 * 
 * PERFORMANCE & OPTIMIZATION DETAILS:
 * - Employs JPQL 'JOIN FETCH' to solve the infamous N+1 query problem in a single step.
 * - Enforces the soft-delete active = true boundary on all fetches.
 * - Supports paginated, sorted, and optional search parameters.
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * Highly optimized dynamic search query that filters active courses.
     * Prevents N+1 queries by fetching category and instructor objects.
     */
    @Query(value = "SELECT c FROM Course c " +
                   "JOIN FETCH c.instructor " +
                   "JOIN FETCH c.category " +
                   "WHERE c.active = true " +
                   "AND (:title IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
                   "AND (:categoryId IS NULL OR c.category.id = :categoryId) " +
                   "AND (:instructorId IS NULL OR c.instructor.id = :instructorId) " +
                   "AND (:maxPrice IS NULL OR c.price <= :maxPrice)",
           countQuery = "SELECT count(c) FROM Course c " +
                        "WHERE c.active = true " +
                        "AND (:title IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
                        "AND (:categoryId IS NULL OR c.category.id = :categoryId) " +
                        "AND (:instructorId IS NULL OR c.instructor.id = :instructorId) " +
                        "AND (:maxPrice IS NULL OR c.price <= :maxPrice)")
    Page<Course> searchCourses(
            @Param("title") String title,
            @Param("categoryId") Long categoryId,
            @Param("instructorId") Long instructorId,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    /**
     * Prevents N+1 lookup when loading details for a specific course by pre-fetching
     * the bi-directional contents module collection, category, and instructor.
     */
    @Query("SELECT c FROM Course c " +
           "LEFT JOIN FETCH c.contents " +
           "JOIN FETCH c.instructor " +
           "JOIN FETCH c.category " +
           "WHERE c.id = :id AND c.active = true")
    Optional<Course> findByIdAndActiveTrue(@Param("id") Long id);

    /**
     * Check if a course is active by ID.
     */
    boolean existsByIdAndActiveTrue(Long id);
}

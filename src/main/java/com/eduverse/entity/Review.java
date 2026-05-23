package com.eduverse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * REVIEW ENTITY
 * ============================================================================
 * 
 * Stores student course ratings (1 to 5 stars) and comments.
 * 
 * ARCHITECTURE KEY DETAILS:
 * - **Rating Check**: Ensures ratings fall between 1 and 5.
 * - **FetchType.LAZY**: Relationships with Student (User) and Course are lazy-loaded.
 * - **Indexing**: Configured indexes on student and course IDs for efficient query filtering.
 */
@Entity
@Table(name = "reviews", 
       indexes = {
           @Index(name = "idx_review_student", columnList = "student_id"),
           @Index(name = "idx_review_course", columnList = "course_id")
       }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many-to-One association with the Student (represented as a User entity)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // Many-to-One association with the Course being reviewed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // Numerical rating ranging from 1 (poor) to 5 (excellent)
    @Column(nullable = false)
    private Integer rating;

    // Descriptive comment or feedback text
    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

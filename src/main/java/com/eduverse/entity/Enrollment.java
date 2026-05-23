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
 * ENROLLMENT ENTITY
 * ============================================================================
 * 
 * Maps student course enrollments in the database.
 * 
 * ARCHITECTURE KEY DETAILS:
 * - **Unique Constraint**: The student_id and course_id combination is declared UNIQUE.
 *   This prevents double-enrollment at the database layer (a crucial safety guard).
 * - **FetchType.LAZY**: Relationships with Student (User) and Course are lazy-loaded.
 * - **Indexing**: Configured indexes on student and course IDs for efficient lookup speeds.
 */
@Entity
@Table(name = "enrollments", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_student_course", columnNames = {"student_id", "course_id"})
       },
       indexes = {
           @Index(name = "idx_enrollment_student", columnList = "student_id"),
           @Index(name = "idx_enrollment_course", columnList = "course_id")
       }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many-to-One association to the Student (represented as a User entity)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // Many-to-One association to the Course
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

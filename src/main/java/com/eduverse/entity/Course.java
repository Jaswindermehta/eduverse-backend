package com.eduverse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * COURSE ENTITY
 * ============================================================================
 * 
 * Represents a complete online course published by an Instructor.
 * 
 * ARCHITECTURE KEY DETAILS:
 * - **Soft Delete Support**: Employs an 'active' flag. Deletion operations toggle this flag
 *   to false instead of running raw physical DELETE statements, maintaining integrity.
 * - **FetchType.LAZY**: Relationships with User (Instructor) and Category are lazy-loaded
 *   to optimize SQL query joins and prevent performance bottle-necks.
 * - **Indexes**: Configured on title, category, and instructor columns to optimize search filtering.
 */
@Entity
@Table(name = "courses", indexes = {
    @Index(name = "idx_course_title", columnList = "title"),
    @Index(name = "idx_course_instructor", columnList = "instructor_id"),
    @Index(name = "idx_course_category", columnList = "category_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 255)
    private String thumbnailUrl;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Soft delete flag: active courses are visible, inactive courses are soft-deleted
    @Column(nullable = false)
    private boolean active = true;

    // Many-to-One with User (representing the Instructor). FetchType.LAZY is specified
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    // Many-to-One with Category. FetchType.LAZY is specified
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // One-to-Many relationship with CourseContent modules. Cascade ALL ensures saving the
    // course will automatically persist/delete its content modules in the database
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseContent> contents = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helper method to add course contents cleanly while preserving bi-directional mapping
    public void addContent(CourseContent content) {
        contents.add(content);
        content.setCourse(this);
    }
}

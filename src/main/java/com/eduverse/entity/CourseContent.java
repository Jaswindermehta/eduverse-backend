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
 * COURSE CONTENT ENTITY
 * ============================================================================
 * 
 * Represents individual lectures, videos, or modules belonging to a Course.
 * 
 * ARCHITECTURE KEY DETAILS:
 * - **FetchType.LAZY**: Relates to Course in a lazy fashion. We don't fetch Course metadata
 *   from database when accessing simple lectures.
 * - **Sequence Ordering**: Holds a sequenceOrder value defining the custom list sequence
 *   of course chapters.
 */
@Entity
@Table(name = "course_contents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many-to-One back-reference to the parent Course
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // The name of this content lecture (e.g. "Section 1: Getting Started")
    @Column(nullable = false, length = 150)
    private String title;

    // File path or URL to the S3 bucket or local fallback folder
    @Column(name = "content_url", nullable = false, length = 255)
    private String contentUrl;

    // Defines the chronological order of contents (e.g. Lesson 1, Lesson 2...)
    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

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
 * CATEGORY ENTITY
 * ============================================================================
 * 
 * Represents a broader educational classification (e.g., "Web Development", "AI/ML").
 * This allows students to search and filter courses by domain topics.
 * 
 * ARCHITECTURE KEY DETAILS:
 * - Includes a database index on the 'name' column for rapid search operations.
 * - Utilizes Hibernate's automatic auditing annotations to capture timestamps.
 */
@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_category_name", columnList = "name", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The unique name of the category (e.g. "Software Engineering")
    @Column(nullable = false, length = 100, unique = true)
    private String name;

    // Description detailing what this category covers
    @Column(length = 255)
    private String description;

    // Automatically records when the category was created in the database
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Automatically updates when any field in this category record is modified
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

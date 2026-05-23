package com.eduverse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * USER DATABASE ENTITY
 * ============================================================================
 * 
 * This class is the core model representing a user (Student, Instructor, or Admin)
 * stored in our PostgreSQL database.
 * 
 * We store details like email, full name, encrypted password, enabled status,
 * and the specific security role link.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Full name of the user (e.g. "John Doe")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    // Email address used for login. Must be unique across the platform.
    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    // Encrypted password (hashed using BCrypt before storing)
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    // A User belongs to one Role. This establishes the Foreign Key relation (role_id).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // Indicates if the account is active or suspended
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    // Timestamps for record keeping
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * ============================================================================
     * JPA LIFECYCLE CALLBACKS
     * ============================================================================
     * 
     * These helper methods are annotated with @PrePersist and @PreUpdate.
     * They are automatically executed by JPA just before database insertion/update,
     * ensuring timestamps are always kept up-to-date automatically without manual setting!
     */
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.enabled = true; // Enabled by default on registration
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

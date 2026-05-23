package com.eduverse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * NOTIFICATION DATABASE ENTITY
 * ============================================================================
 * 
 * Maps user notifications (alerts) to the PostgreSQL database.
 * Used to store messages intended for students or instructors.
 */
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Recipient of this alert (Student/Instructor)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The heading title of the notification
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    // The full body text of the alert
    @Column(name = "message", nullable = false, length = 500)
    private String message;

    // Tracker to check if the user has viewed the notification
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    // Creation timestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }
}

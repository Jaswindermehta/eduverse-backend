package com.eduverse.repository;

import com.eduverse.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * ============================================================================
 * NOTIFICATION DATABASE REPOSITORY
 * ============================================================================
 * 
 * Maps all standard database operations for the "Notification" entity.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Retrieve all alerts matching the user's ID, sorted newest first.
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Retrieve read or unread alerts matching the user's ID, sorted newest first.
     */
    List<Notification> findByUserIdAndReadOrderByCreatedAtDesc(Long userId, boolean read);
}

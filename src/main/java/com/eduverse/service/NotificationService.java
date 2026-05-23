package com.eduverse.service;

import com.eduverse.dto.NotificationDto;
import com.eduverse.entity.Notification;
import com.eduverse.entity.User;
import com.eduverse.exception.ResourceNotFoundException;
import com.eduverse.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * NOTIFICATION SERVICE
 * ============================================================================
 * 
 * Handles all business logic, transactions, and transformations for persistent 
 * database-backed user alerts (notifications).
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Retrieves notifications for a specific user.
     * Optionally filters only unread notifications.
     */
    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(Long userId, boolean unreadOnly) {
        logger.debug("Fetching notifications for user ID: {}, unreadOnly = {}", userId, unreadOnly);
        
        List<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false);
        } else {
            notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        return notifications.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Marks a specific notification as read.
     * Asserts that the notification belongs to the requesting username for safety.
     */
    @Transactional
    public NotificationDto markAsRead(Long notificationId, String requestingUsername) {
        logger.info("Marking notification ID {} as read by user: {}", notificationId, requestingUsername);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        // Security check: ensure notification recipient matches the authenticated user
        if (!notification.getUser().getEmail().equals(requestingUsername)) {
            logger.error("Security violation: User '{}' tried to mark Notification ID '{}' (belonging to '{}') as read",
                    requestingUsername, notificationId, notification.getUser().getEmail());
            throw new AccessDeniedException("You are not authorized to access this notification.");
        }

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        logger.debug("Notification ID {} successfully marked as read", notificationId);
        
        return mapToDto(saved);
    }

    /**
     * Low-level method to persist a new notification in the database.
     * Used by listeners and background event workers.
     */
    @Transactional
    public Notification createNotification(User user, String title, String message) {
        logger.info("Creating notification alert for user '{}' [{}]", user.getEmail(), title);
        
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    /**
     * Maps a Notification entity to a NotificationDto.
     */
    private NotificationDto mapToDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getUser().getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}

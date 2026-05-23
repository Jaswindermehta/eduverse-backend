package com.eduverse.controller;

import com.eduverse.dto.ApiResponse;
import com.eduverse.dto.NotificationDto;
import com.eduverse.entity.User;
import com.eduverse.repository.UserRepository;
import com.eduverse.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * ============================================================================
 * NOTIFICATION API CONTROLLER
 * ============================================================================
 * 
 * Provides RESTful API endpoints for students and instructors to view and 
 * manage their persistent in-app notifications (alerts).
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications Module", description = "Endpoints for students and instructors to retrieve and manage alerts.")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /**
     * Retrieve all alerts for the currently authenticated user session.
     */
    @GetMapping
    @Operation(summary = "Get user notifications", description = "Fetches a list of in-app notification alerts for the logged-in student or instructor.")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getMyNotifications(
            @RequestParam(value = "unreadOnly", defaultValue = "false") boolean unreadOnly
    ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Retrieving notifications for session: {}, unreadOnly = {}", username, unreadOnly);

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found in system."));

        List<NotificationDto> notifications = notificationService.getUserNotifications(user.getId(), unreadOnly);
        return ResponseEntity.ok(ApiResponse.success("Notifications successfully retrieved.", notifications));
    }

    /**
     * Mark a specific notification alert as read.
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks a specific persistent alert as read. Asserts ownership.")
    public ResponseEntity<ApiResponse<NotificationDto>> markNotificationAsRead(
            @PathVariable("id") Long id
    ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Request to mark notification ID '{}' as read by user '{}'", id, username);

        NotificationDto updatedDto = notificationService.markAsRead(id, username);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read.", updatedDto));
    }
}

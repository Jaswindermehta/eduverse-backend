package com.eduverse.event.listener;

import com.eduverse.entity.Enrollment;
import com.eduverse.entity.Review;
import com.eduverse.event.EnrollmentCreatedEvent;
import com.eduverse.event.ReviewCreatedEvent;
import com.eduverse.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * NOTIFICATION EVENT LISTENER
 * ============================================================================
 * 
 * Captures core system events asynchronously (`@Async`) and persists in-app alerts 
 * in the PostgreSQL database using NotificationService.
 */
@Component
public class NotificationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Listens for student enrollments.
     * Persists a success alert for the Student, and a signup alert for the Instructor.
     */
    @Async("taskExecutor")
    @EventListener
    public void handleEnrollmentEvent(EnrollmentCreatedEvent event) {
        Enrollment enrollment = event.getEnrollment();
        
        logger.debug("[EVENT LISTENER] Processing persistent alerts for enrollment ID: {}", enrollment.getId());

        // 1. Student notification
        String studentTitle = "Enrollment Successful! 🎉";
        String studentMsg = String.format("You have successfully registered for '%s'. Get started in your dashboard!", 
                enrollment.getCourse().getTitle());
        notificationService.createNotification(enrollment.getStudent(), studentTitle, studentMsg);

        // 2. Instructor notification
        String instructorTitle = "New Student Registration 📈";
        String instructorMsg = String.format("Great news! Student '%s' has enrolled in your course: '%s'.", 
                enrollment.getStudent().getFullName(), enrollment.getCourse().getTitle());
        notificationService.createNotification(enrollment.getCourse().getInstructor(), instructorTitle, instructorMsg);
    }

    /**
     * Listens for reviews.
     * Persists an alert for the Instructor with the rating received.
     */
    @Async("taskExecutor")
    @EventListener
    public void handleReviewEvent(ReviewCreatedEvent event) {
        Review review = event.getReview();

        logger.debug("[EVENT LISTENER] Processing persistent alert for review ID: {}", review.getId());

        String instructorTitle = "New Course Review ⭐";
        String instructorMsg = String.format("Student '%s' gave a %.1f-star rating on your course '%s': \"%s\"",
                review.getStudent().getFullName(), 
                review.getRating(), 
                review.getCourse().getTitle(),
                review.getComment()
        );
        
        notificationService.createNotification(review.getCourse().getInstructor(), instructorTitle, instructorMsg);
    }
}

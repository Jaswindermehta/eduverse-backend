package com.eduverse.event.listener;

import com.eduverse.entity.Enrollment;
import com.eduverse.entity.Review;
import com.eduverse.event.EnrollmentCreatedEvent;
import com.eduverse.event.ReviewCreatedEvent;
import com.eduverse.service.AsyncEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * EMAIL EVENT LISTENER
 * ============================================================================
 * 
 * Captures core system events asynchronously (`@Async`) and triggers HTML email
 * templates dispatch via AsyncEmailService.
 */
@Component
public class EmailEventListener {

    private static final Logger logger = LoggerFactory.getLogger(EmailEventListener.class);

    private final AsyncEmailService emailService;

    public EmailEventListener(AsyncEmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Listens for student enrollments and sends a stylized HTML welcome email.
     */
    @Async("taskExecutor")
    @EventListener
    public void handleEnrollmentEvent(EnrollmentCreatedEvent event) {
        Enrollment enrollment = event.getEnrollment();
        String studentEmail = enrollment.getStudent().getEmail();
        String studentName = enrollment.getStudent().getFullName();
        String courseTitle = enrollment.getCourse().getTitle();

        logger.debug("[EVENT LISTENER] Processing enrollment welcome email for student: {}", studentEmail);

        String subject = "Welcome to " + courseTitle + " | Eduverse";
        String htmlContent = String.format(
                "<html>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "  <div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;'>" +
                "    <h2 style='color: #4F46E5;'>Congratulations, %s! 🎉</h2>" +
                "    <p>You have successfully enrolled in the course: <strong>%s</strong>.</p>" +
                "    <p>Your learning journey starts now! You can access your materials and lectures directly from your learning dashboard at any time.</p>" +
                "    <br/>" +
                "    <a href='http://localhost:8080/swagger-ui.html' style='display: inline-block; padding: 10px 20px; background-color: #4F46E5; color: #fff; text-decoration: none; border-radius: 5px;'>Go to Classroom</a>" +
                "    <hr style='border: none; border-top: 1px solid #eaeaea; margin: 20px 0;'/>" +
                "    <p style='font-size: 12px; color: #777;'>Eduverse Inc. - The premier online learning portal.</p>" +
                "  </div>" +
                "</body>" +
                "</html>",
                studentName, courseTitle
        );

        emailService.sendHtmlEmail(studentEmail, subject, htmlContent);
    }

    /**
     * Listens for reviews left on courses and alerts the respective instructor.
     */
    @Async("taskExecutor")
    @EventListener
    public void handleReviewEvent(ReviewCreatedEvent event) {
        Review review = event.getReview();
        String instructorEmail = review.getCourse().getInstructor().getEmail();
        String instructorName = review.getCourse().getInstructor().getFullName();
        String reviewerName = review.getStudent().getFullName();
        String courseTitle = review.getCourse().getTitle();
        double rating = review.getRating();
        String feedback = review.getComment();

        logger.debug("[EVENT LISTENER] Processing review notification email for instructor: {}", instructorEmail);

        String subject = "New Student Review for " + courseTitle + " | Eduverse";
        String htmlContent = String.format(
                "<html>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "  <div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;'>" +
                "    <h2 style='color: #4F46E5;'>Hello, %s! 🌟</h2>" +
                "    <p>Your course <strong>%s</strong> just received a new student review!</p>" +
                "    <div style='background-color: #f9f9f9; padding: 15px; border-left: 4px solid #4F46E5; margin: 15px 0;'>" +
                "      <p style='margin: 0; font-weight: bold;'>Rating: %s / 5.0 ⭐</p>" +
                "      <p style='margin: 10px 0 0 0; font-style: italic;'>\"%s\"</p>" +
                "      <p style='margin: 5px 0 0 0; font-size: 12px; color: #666;'>— left by %s</p>" +
                "    </div>" +
                "    <p>Check out your instructor portal to reply to the feedback.</p>" +
                "    <hr style='border: none; border-top: 1px solid #eaeaea; margin: 20px 0;'/>" +
                "    <p style='font-size: 12px; color: #777;'>Eduverse Instructor Portal Alerts.</p>" +
                "  </div>" +
                "</body>" +
                "</html>",
                instructorName, courseTitle, rating, feedback, reviewerName
        );

        emailService.sendHtmlEmail(instructorEmail, subject, htmlContent);
    }
}

package com.eduverse.event;

import com.eduverse.entity.Enrollment;
import org.springframework.context.ApplicationEvent;

/**
 * ============================================================================
 * ENROLLMENT CREATED APPLICATION EVENT
 * ============================================================================
 * 
 * An immutable application event published when a Student enrolls in a Course.
 * 
 * ----------------------------------------------------------------------------
 * BEGINNER-FRIENDLY EXPLANATION: EVENT-DRIVEN ARCHITECTURE & LOOSE COUPLING
 * ----------------------------------------------------------------------------
 * - **Tightly Coupled (Bad)**: When a student enrolls, EnrollmentService has to
 *   directly call EmailService.sendWelcomeEmail() and NotificationService.createAlert().
 *   If we want to add a third task (like updating a scoreboard or Slack alert), we
 *   must modify EnrollmentService, risking breaking the core enrollment transaction.
 * - **Loosely Coupled (Good)**: With Event-Driven Architecture, EnrollmentService
 *   just publishes an "EnrollmentCreatedEvent" and is done! It has no idea who is
 *   listening or what they will do with it.
 * - **Listeners**: Independent components "listen" for this event and perform tasks.
 *   If one listener crashes, the core enrollment still completes perfectly. Adding new 
 *   features is as simple as adding a new listener class.
 */
public class EnrollmentCreatedEvent extends ApplicationEvent {

    private final Enrollment enrollment;

    public EnrollmentCreatedEvent(Object source, Enrollment enrollment) {
        super(source);
        this.enrollment = enrollment;
    }

    public Enrollment getEnrollment() {
        return enrollment;
    }
}

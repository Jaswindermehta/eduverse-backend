package com.eduverse.event;

import com.eduverse.entity.Review;
import org.springframework.context.ApplicationEvent;

/**
 * ============================================================================
 * REVIEW CREATED APPLICATION EVENT
 * ============================================================================
 * 
 * An immutable application event published when a Student leaves a Course Review.
 * 
 * Used to trigger background updates and notifications to the Course Instructor.
 */
public class ReviewCreatedEvent extends ApplicationEvent {

    private final Review review;

    public ReviewCreatedEvent(Object source, Review review) {
        super(source);
        this.review = review;
    }

    public Review getReview() {
        return review;
    }
}

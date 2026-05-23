package com.eduverse.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * REVIEW RESPONSE DTO
 * ============================================================================
 * 
 * Simple data container for returning formatted course reviews.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;

    private Long studentId;
    
    private String studentName;

    private Long courseId;
    
    private Integer rating;
    
    private String comment;

    private LocalDateTime createdAt;
}

package com.eduverse.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * ENROLLMENT RESPONSE DTO
 * ============================================================================
 * 
 * Simple data container for returning student enrollment confirmation payloads.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {

    private Long id;

    private Long studentId;
    
    private String studentName;

    private Long courseId;
    
    private String courseTitle;

    private LocalDateTime enrolledAt;
}

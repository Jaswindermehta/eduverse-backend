package com.eduverse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * NOTIFICATION DATA TRANSFER OBJECT (DTO)
 * ============================================================================
 * 
 * Simple data container for returning user alert payloads from endpoints.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private Long id;
    private Long userId;
    private String title;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
}

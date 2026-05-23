package com.eduverse.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * CATEGORY DTO
 * ============================================================================
 * 
 * Simple, safe data container for transferring Category information
 * in API request/responses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {

    private Long id;
    
    private String name;
    
    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

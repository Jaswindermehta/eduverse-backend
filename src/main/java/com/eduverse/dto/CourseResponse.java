package com.eduverse.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================================
 * COURSE RESPONSE DTO
 * ============================================================================
 * 
 * Primary course payload returned in GET API responses.
 * 
 * ARCHITECTURE KEY DETAILS:
 * - Decouples database entities from consumer payloads completely.
 * - Flat-maps instructor and category properties for clean UI consumption.
 * - **Dynamic Aggregations**: Exposes computed averageRating and totalReviews statistics.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {

    private Long id;
    
    private String title;
    
    private String description;
    
    private String thumbnailUrl;
    
    private BigDecimal price;

    private boolean active;

    // Decoupled Instructor parameters (instead of exposing User object)
    private Long instructorId;
    private String instructorName;

    // Decoupled Category parameters (instead of exposing Category object)
    private Long categoryId;
    private String categoryName;

    // Embedded lecture modules list
    private List<CourseContentDto> contents;

    // Computed ratings fields
    private Double averageRating = 0.0;
    private Integer totalReviews = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

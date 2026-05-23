package com.eduverse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * ============================================================================
 * COURSE CONTENT DTO
 * ============================================================================
 * 
 * Simple data container for transferring course lessons/modules data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseContentDto {

    private Long id;

    @NotBlank(message = "Content title is required")
    private String title;

    @NotBlank(message = "Content URL/S3 link is required")
    private String contentUrl;

    @NotNull(message = "Sequence order is required")
    private Integer sequenceOrder;
}

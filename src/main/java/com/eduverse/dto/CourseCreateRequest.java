package com.eduverse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

/**
 * ============================================================================
 * COURSE CREATE REQUEST DTO
 * ============================================================================
 * 
 * Captures request payload from instructors creating a new course.
 * Includes complete JSR-380 field-validation constraints.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseCreateRequest {

    @NotBlank(message = "Course title is required")
    private String title;

    @NotBlank(message = "Course description is required")
    private String description;

    private String thumbnailUrl;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
    private BigDecimal price;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    // Bi-directional validation checks for all nested lecture units
    @NotEmpty(message = "Course must have at least one content module")
    @Valid
    private List<CourseContentDto> contents;
}

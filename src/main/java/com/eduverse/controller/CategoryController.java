package com.eduverse.controller;

import com.eduverse.dto.ApiResponse;
import com.eduverse.dto.CategoryDto;
import com.eduverse.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * ============================================================================
 * CATEGORY CONTROLLER
 * ============================================================================
 * 
 * Exposes RESTful endpoints for managing and retrieving course categories.
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "Category Management", description = "Endpoints for creating and browsing course categories")
public class CategoryController {

    private final CategoryService categoryService;

    // Constructor Injection
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Public endpoint to fetch all categories.
     */
    @GetMapping
    @Operation(summary = "Get all categories", description = "Publicly retrieves a list of all course categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getAllCategories() {
        List<CategoryDto> categories = categoryService.getAllCategories();
        
        ApiResponse<List<CategoryDto>> response = new ApiResponse<>(
                true,
                "Categories retrieved successfully.",
                categories
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Admin-only endpoint to create a category.
     */
    @PostMapping
    @Operation(summary = "Create a new category (Admin Only)", description = "Registers a new course category. Locked to ADMIN role.")
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(@Valid @RequestBody CategoryDto categoryDto) {
        CategoryDto created = categoryService.createCategory(categoryDto);
        
        ApiResponse<CategoryDto> response = new ApiResponse<>(
                true,
                "Category successfully created.",
                created
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

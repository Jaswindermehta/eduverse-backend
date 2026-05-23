package com.eduverse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ============================================================================
 * GLOBAL API RESPONSE WRAPPER
 * ============================================================================
 * 
 * This is a highly reusable "Wrapper" class. Instead of returning raw entities
 * or strings directly from controllers, we wrap ALL of our API responses in
 * this single, standard structural format.
 * 
 * It ensures that the client (Frontend, iOS, Android) always receives a consistent
 * JSON response structure like:
 * {
 *    "success": true,
 *    "message": "Action completed successfully",
 *    "data": { ... }
 * }
 * 
 * We use Lombok:
 * - @Data: Automatically generates getters and setters.
 * - @Builder: Implements the builder pattern so we can write:
 *            ApiResponse.builder().success(true).message("Hello").build();
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    // Indicates if the request was processed successfully (true/false)
    private boolean success;

    // A helpful message explaining the result (e.g. "Login successful")
    private String message;

    // The actual data payload returned to the client (User, Token, Course list, etc.)
    // We use a generic type 'T' so it can hold ANY Java object!
    private T data;

    /**
     * Helper static method to create a quick success response with data.
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Helper static method to create a quick success response with no data payload.
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(null)
                .build();
    }

    /**
     * Helper static method to create a quick error response.
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}

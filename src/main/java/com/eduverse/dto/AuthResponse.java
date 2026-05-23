package com.eduverse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ============================================================================
 * AUTHENTICATION RESPONSE DTO
 * ============================================================================
 * 
 * This class represents the final JSON payload returned on a successful login
 * or registration attempt.
 * 
 * It packages:
 * 1. The generated JWT Token (used as the secure authorization stamp).
 * 2. The User profile DTO (safe info displaying user's name and role).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    // The JWT Access Token string
    private String token;

    // Type of token prefix, typically "Bearer" in modern API standards
    @Builder.Default
    private String tokenType = "Bearer";

    // Safe details of the logged-in user
    private UserDto user;
}

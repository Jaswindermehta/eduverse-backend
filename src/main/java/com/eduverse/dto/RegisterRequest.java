package com.eduverse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ============================================================================
 * REGISTRATION REQUEST DTO
 * ============================================================================
 * 
 * This class represents the JSON object that a client (frontend) sends to our
 * system when a new user tries to register / sign up.
 * 
 * It contains inputs for full name, email, password, and the requested role name.
 * 
 * We use Jakarta validation constraints here so that Spring will validate the
 * incoming payload automatically before it ever enters our business service logic!
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    // Validates that full name is not empty and is a reasonable length
    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    private String fullName;

    // Checks that the email is not blank and is formatted as a valid email address
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    // Restricts password length for basic security integrity
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters long")
    private String password;

    // The user must specify what role they want: "STUDENT" or "INSTRUCTOR". 
    // Admin roles are not allowed to be registered via public APIs for security reasons!
    @NotBlank(message = "Role is required")
    private String role;
}

package com.eduverse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ============================================================================
 * LOGIN REQUEST DTO
 * ============================================================================
 * 
 * This class represents the JSON object sent by the client when they try
 * to authenticate (login) into the Eduverse platform.
 * 
 * We validate that both fields are present and that email conforms to standard
 * formats before processing in the authentication service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}

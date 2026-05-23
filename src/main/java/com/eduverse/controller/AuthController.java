package com.eduverse.controller;

import com.eduverse.dto.ApiResponse;
import com.eduverse.dto.AuthResponse;
import com.eduverse.dto.LoginRequest;
import com.eduverse.dto.RegisterRequest;
import com.eduverse.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ============================================================================
 * AUTHENTICATION REST API GATEWAY (CONTROLLER)
 * ============================================================================
 * 
 * This class exposes our public REST API endpoints for user registration
 * and log-in.
 * 
 * Flow:
 * Client Request (JSON) -> AuthController (Validates Input DTO) -> AuthService (Executes Logic) -> PostgreSQL Database
 * 
 * We return the standard ResponseEntity<ApiResponse<T>> wrapper format for all
 * successful responses.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    // Constructor Injection
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Public endpoint to register a new user (STUDENT or INSTRUCTOR).
     * 
     * @param registerRequest Holds validated details of the new user.
     * @return 201 Created with standard success JSON and JWT access token.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        logger.info("REST API Request received: Register user with email: {}", registerRequest.getEmail());
        
        // Execute business registration logic
        AuthResponse responseData = authService.registerUser(registerRequest);
        
        // Wrap payload in our consistent ApiResponse container
        ApiResponse<AuthResponse> apiResponse = ApiResponse.success(
                "User account registered successfully! Welcome to Eduverse.",
                responseData
        );

        // Return with '201 Created' HTTP status code (standard REST practice for creations)
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    /**
     * Public endpoint to authenticate existing users.
     * 
     * @param loginRequest Holds validated email and password credentials.
     * @return 200 OK with standard success JSON and new JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("REST API Request received: Login user with email: {}", loginRequest.getEmail());
        
        // Verify credentials and generate session details
        AuthResponse responseData = authService.loginUser(loginRequest);
        
        // Wrap response
        ApiResponse<AuthResponse> apiResponse = ApiResponse.success(
                "Login successful! Session authenticated.",
                responseData
        );

        // Return with '200 OK' HTTP status code (standard REST practice for successful queries)
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}

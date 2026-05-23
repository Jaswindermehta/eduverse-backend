package com.eduverse.exception;

import com.eduverse.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * GLOBAL CENTRALIZED EXCEPTION HANDLER
 * ============================================================================
 * 
 * In a professional Spring Boot application, we never want raw server errors,
 * database stack traces, or complex exception logs to escape to the client.
 * Doing so is a major security risk and provides a terrible user experience.
 * 
 * This class uses @ControllerAdvice to listen across all our controllers.
 * Whenever an exception is thrown inside controllers or services, it will be
 * captured here, logged using SLF4J, wrapped in a uniform, structured
 * ApiResponse<Void>, and returned with the correct HTTP status code!
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // Logger instance to print errors in the server console using SLF4J
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles custom ResourceNotFoundException.
     * Returns a "404 Not Found" HTTP response.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("Resource not found exception intercepted: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles custom InvalidCredentialsException.
     * Returns a "401 Unauthorized" HTTP response.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles Spring Security AccessDeniedException (e.g. STUDENT trying to call ADMIN APIs).
     * Returns a "403 Forbidden" HTTP response.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Security Access Denied intercepted: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.error("Access Denied: You do not have permissions to access this API endpoint!");
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles input validation errors (triggered by @Valid annotation on requests).
     * Returns a "400 Bad Request" HTTP response containing all specific failed fields.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("Request input validation failed on {} fields", ex.getBindingResult().getErrorCount());

        // Extract and format all field validation errors (e.g. "email: Must be valid")
        String validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        ApiResponse<Void> response = ApiResponse.error("Input validation failed: " + validationErrors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles custom illegal argument and state errors.
     * Returns a "400 Bad Request" HTTP response.
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(RuntimeException ex) {
        logger.warn("Bad request exception intercepted: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles security scan blocks and path traversal violations.
     * Returns a "400 Bad Request" HTTP response.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityException(SecurityException ex) {
        logger.warn("Security policy violation intercepted: {}", ex.getMessage());
        
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Catch-all handler for any unexpected system exceptions (e.g. NullPointerException, Database failures).
     * Returns a "500 Internal Server Error" HTTP response to protect internal code details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        // Log the full stack trace on the server for debugging
        logger.error("An unexpected internal server error occurred!", ex);

        ApiResponse<Void> response = ApiResponse.error("An internal server error occurred. Please contact the administrator.");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

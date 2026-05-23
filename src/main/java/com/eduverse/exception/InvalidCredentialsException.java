package com.eduverse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ============================================================================
 * CUSTOM INVALID CREDENTIALS EXCEPTION
 * ============================================================================
 * 
 * This custom exception is thrown during the login flow if:
 * 1. The provided email is not registered.
 * 2. The provided password does not match the encrypted password stored in the database.
 * 
 * Annotated with @ResponseStatus(HttpStatus.UNAUTHORIZED) to return a clean
 * "401 Unauthorized" HTTP status code.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCredentialsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor accepting a detailed error message.
     * 
     * @param message Explains what credential checks failed.
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }
}

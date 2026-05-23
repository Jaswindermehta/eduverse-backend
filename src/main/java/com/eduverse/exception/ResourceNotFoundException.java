package com.eduverse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ============================================================================
 * CUSTOM RESOURCE NOT FOUND EXCEPTION
 * ============================================================================
 * 
 * We throw this custom exception whenever we lookup a record in the database
 * (like a User, Role, or Course) and it is not found.
 * 
 * We add @ResponseStatus(HttpStatus.NOT_FOUND) so that if this exception escapes
 * our service layer, Spring will automatically map it to a standard "404 Not Found"
 * HTTP status code!
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private String resourceName;
    private String fieldName;
    private Object fieldValue;

    /**
     * Constructor accepting a detailed error message.
     * 
     * @param message Explains what resource was missing.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor for field-based error reporting.
     *
     * @param resourceName Name of the resource (e.g. Course)
     * @param fieldName Name of the lookup field (e.g. id)
     * @param fieldValue Value of the lookup field (e.g. 1)
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}

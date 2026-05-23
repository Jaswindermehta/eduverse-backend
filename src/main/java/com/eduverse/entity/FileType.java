package com.eduverse.entity;

import java.util.List;

/**
 * ============================================================================
 * FILE TYPE ENUM
 * ============================================================================
 * 
 * Declares strict rules, size constraints, and allowed extensions/MIME types
 * for different category uploads inside Eduverse.
 * 
 * Each FileType defines:
 * 1. Maximum file size in bytes
 * 2. Whitelisted file extensions
 * 3. Whitelisted official MIME Content-Types
 */
public enum FileType {
    
    THUMBNAIL(
        5 * 1024 * 1024L, // 5 Megabytes
        List.of("jpg", "jpeg", "png"),
        List.of("image/jpeg", "image/png")
    ),
    
    DOCUMENT(
        15 * 1024 * 1024L, // 15 Megabytes
        List.of("pdf"),
        List.of("application/pdf")
    ),
    
    RESOURCE(
        25 * 1024 * 1024L, // 25 Megabytes
        List.of("pdf", "zip"),
        List.of("application/pdf", "application/zip")
    ),
    
    VIDEO(
        100 * 1024 * 1024L, // 100 Megabytes
        List.of("mp4"),
        List.of("video/mp4")
    );

    private final long maxSize;
    private final List<String> allowedExtensions;
    private final List<String> allowedMimeTypes;

    FileType(long maxSize, List<String> allowedExtensions, List<String> allowedMimeTypes) {
        this.maxSize = maxSize;
        this.allowedExtensions = allowedExtensions;
        this.allowedMimeTypes = allowedMimeTypes;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public List<String> getAllowedMimeTypes() {
        return allowedMimeTypes;
    }
}

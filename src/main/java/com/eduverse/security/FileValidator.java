package com.eduverse.security;

import com.eduverse.entity.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.Objects;

/**
 * ============================================================================
 * SECURE FILE VALIDATOR COMPONENT
 * ============================================================================
 * 
 * Inspects incoming multipart files against rigorous validation matrices
 * defined in FileType.java. 
 * 
 * Protects the system against MIME type spoofing and oversized disk abuse.
 */
@Component
public class FileValidator {

    private static final Logger logger = LoggerFactory.getLogger(FileValidator.class);

    /**
     * Runs full structural file validation rules.
     * Throws IllegalArgumentException if any criteria is violated.
     */
    public void validate(MultipartFile file, FileType fileType) {
        logger.debug("Entering security validation pipeline for file type: {}", fileType);

        // 1. Presence check
        if (file == null || file.isEmpty()) {
            logger.error("File upload validation failed: raw payload is empty");
            throw new IllegalArgumentException("Upload failed: File is required and cannot be empty.");
        }

        // 2. Max Size check
        if (file.getSize() > fileType.getMaxSize()) {
            logger.error("File validation failed: size {} exceeds maximum limits {} for type {}", 
                    file.getSize(), fileType.getMaxSize(), fileType);
            throw new IllegalArgumentException(String.format(
                    "Upload failed: File size exceeds the maximum limit of %d MB.", 
                    fileType.getMaxSize() / (1024 * 1024)
            ));
        }

        // 3. Extension extraction and check
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            logger.error("File validation failed: missing original filename");
            throw new IllegalArgumentException("Upload failed: Missing filename.");
        }

        String extension = getFileExtension(originalFilename);
        if (!fileType.getAllowedExtensions().contains(extension.toLowerCase())) {
            logger.error("File validation failed: extension '{}' forbidden for type {}", extension, fileType);
            throw new IllegalArgumentException(String.format(
                    "Upload failed: Invalid file extension. Allowed extensions are: %s", 
                    String.join(", ", fileType.getAllowedExtensions())
            ));
        }

        // 4. ContentType MIME spoof check
        String contentType = file.getContentType();
        if (contentType == null || !fileType.getAllowedMimeTypes().contains(contentType.toLowerCase())) {
            logger.error("MIME type spoof detected: content-type '{}' is invalid for type {}", contentType, fileType);
            throw new IllegalArgumentException(String.format(
                    "Upload failed: Invalid file content type. Allowed MIME categories: %s", 
                    String.join(", ", fileType.getAllowedMimeTypes())
            ));
        }

        logger.info("File upload security validation checks passed successfully for file: {}", originalFilename);
    }

    /**
     * Extracts extension safely from clean filename string.
     */
    private String getFileExtension(String filename) {
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // No extension
        }
        return filename.substring(lastIndexOf + 1);
    }
}

package com.eduverse.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * ============================================================================
 * FILE STORAGE STRATEGY INTERFACE
 * ============================================================================
 * 
 * Declares the standard API contracts for file management operations.
 * 
 * Implements the **Strategy Design Pattern**, letting the application switch
 * between local disk storage and AWS S3 bucket storage dynamically at runtime.
 */
public interface FileStorageService {

    /**
     * Uploads/Saves the multipart file inside the targeted folder bucket.
     * 
     * @param file Incoming raw multipart file payload.
     * @param folder Target folder directory path (e.g. "thumbnails", "resources").
     * @return Fully resolved string URL path to access the stored file.
     */
    String storeFile(MultipartFile file, String folder);

    /**
     * Deletes the file matching the provided URL/Key from storage.
     * 
     * @param fileUrl Fully resolved string path or storage key.
     */
    void deleteFile(String fileUrl);

    /**
     * Generates a temporary pre-signed URL to access private files securely.
     * E.g. allowing direct S3 retrieval without exposing direct bucket links.
     * 
     * If the concrete strategy does not support pre-signed URLs (like local storage),
     * it will fall back to returning the direct access path.
     * 
     * @param fileKey Storage key path of the target file.
     * @param expirationInMinutes Lifespan of the pre-signed URL.
     * @return Fully qualified access URL.
     */
    String generatePresignedUrl(String fileKey, int expirationInMinutes);
}

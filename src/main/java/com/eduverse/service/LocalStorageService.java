package com.eduverse.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * ============================================================================
 * LOCAL STORAGE CONCRETE STRATEGY
 * ============================================================================
 * 
 * Writes uploads to the local server disk file system.
 * Serves as our primary fallback strategy when AWS cloud keys are unavailable.
 * 
 * SECURITY IMPLEMENTATIONS:
 * - **Path Traversal Protection**: Asserts that file operations are strictly contained
 *   within the configured root upload folder.
 * - **Unique Filenames**: Generates secure random UUID names to prevent filename collisions.
 */
@Service("localStorageService")
public class LocalStorageService implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path fileStorageLocation;

    public LocalStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        // Resolve absolute path of upload directory and create if absent
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
            logger.info("Local storage fallback directory initialized at: {}", this.fileStorageLocation);
        } catch (IOException ex) {
            logger.error("Could not create local storage fallback directory!", ex);
            throw new RuntimeException("Could not initialize local storage fallback directory.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String folder) {
        // Extract suffix extension safely (e.g. "png")
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Generate secure random UUID name (prevents collisions and path injections)
        String secureFileName = UUID.randomUUID().toString() + extension;
        
        try {
            // Target folder location (e.g. uploads/thumbnails)
            Path targetDirectory = this.fileStorageLocation.resolve(folder).normalize();
            Files.createDirectories(targetDirectory);

            // Complete absolute path target file location
            Path targetFile = targetDirectory.resolve(secureFileName).normalize();

            // Path Traversal Security check: verify target path starts with root folder
            if (!targetFile.startsWith(this.fileStorageLocation)) {
                logger.error("Security alert: Attempted path traversal attack with folder path: {}", folder);
                throw new SecurityException("Security violation: Path traversal is forbidden.");
            }

            // Copy file input stream to the target local path
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            logger.info("File successfully written to local disk storage: {}/{}", folder, secureFileName);

            // Return relative access URL
            return "/uploads/" + folder + "/" + secureFileName;
        } catch (IOException ex) {
            logger.error("Could not write file to local disk!", ex);
            throw new RuntimeException("Could not store file on local disk. Please try again.", ex);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        logger.info("Attempting local file deletion for URL: {}", fileUrl);

        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            logger.warn("Deletion bypassed: URL '{}' does not represent a local disk file.", fileUrl);
            return;
        }

        // Extract relative file path from URL
        String relativePath = fileUrl.replace("/uploads/", "");
        try {
            Path targetFile = this.fileStorageLocation.resolve(relativePath).normalize();

            // Safety path traversal check
            if (!targetFile.startsWith(this.fileStorageLocation)) {
                logger.error("Security violation attempted inside file deletion: {}", fileUrl);
                throw new SecurityException("Security violation: Unauthorized path deletion access.");
            }

            if (Files.exists(targetFile)) {
                Files.delete(targetFile);
                logger.info("Successfully deleted local disk file: {}", targetFile);
            } else {
                logger.warn("Local file deletion failed: file does not exist at {}", targetFile);
            }
        } catch (IOException ex) {
            logger.error("Failed to delete local disk file!", ex);
        }
    }

    @Override
    public String generatePresignedUrl(String fileKey, int expirationInMinutes) {
        logger.debug("Pre-signed URL generated for local file (direct fallback path returned): {}", fileKey);
        // Local storage does not support pre-signed keys, so we return the direct URL key
        return fileKey;
    }
}

package com.eduverse.controller;

import com.eduverse.dto.ApiResponse;
import com.eduverse.dto.UploadResponse;
import com.eduverse.entity.FileType;
import com.eduverse.entity.UploadMetadata;
import com.eduverse.entity.User;
import com.eduverse.repository.UploadMetadataRepository;
import com.eduverse.repository.UserRepository;
import com.eduverse.security.FileSecurityScanner;
import com.eduverse.security.FileValidator;
import com.eduverse.security.UploadRateLimiter;
import com.eduverse.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * ============================================================================
 * SECURE FILE UPLOAD CONTROLLER
 * ============================================================================
 * 
 * Provides RESTful API endpoints for managing course attachment uploads (thumbnails, PDFs, etc.).
 * Integrates rate limiting, malware security scanning, file structure validation,
 * and database telemetry tracking.
 */
@RestController
@RequestMapping("/api/uploads")
@Tag(name = "Uploads Module", description = "Handles secure file uploads to cloud S3 or local fallback storage strategy.")
public class UploadController {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);

    private final FileStorageService fileStorageService;
    private final FileValidator fileValidator;
    private final FileSecurityScanner fileSecurityScanner;
    private final UploadRateLimiter uploadRateLimiter;
    private final UserRepository userRepository;
    private final UploadMetadataRepository uploadMetadataRepository;

    public UploadController(
            FileStorageService fileStorageService,
            FileValidator fileValidator,
            FileSecurityScanner fileSecurityScanner,
            UploadRateLimiter uploadRateLimiter,
            UserRepository userRepository,
            UploadMetadataRepository uploadMetadataRepository
    ) {
        this.fileStorageService = fileStorageService;
        this.fileValidator = fileValidator;
        this.fileSecurityScanner = fileSecurityScanner;
        this.uploadRateLimiter = uploadRateLimiter;
        this.userRepository = userRepository;
        this.uploadMetadataRepository = uploadMetadataRepository;
    }

    /**
     * Upload a new attachment.
     * Consumes multipart/form-data.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file", description = "Securely uploads a thumbnail, document, resource, or video, performing full multi-stage validation.")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") FileType type,
            @RequestParam(value = "folder", defaultValue = "general") String folder
    ) {
        // Retrieve current authenticated user session details
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Received upload request from user '{}' for file '{}' of type '{}'", 
                username, file.getOriginalFilename(), type);

        // 1. Rate Limiting Check (Token-Bucket)
        if (!uploadRateLimiter.allowUpload(username)) {
            logger.warn("Upload block: User '{}' exceeded upload frequency rate limits.", username);
            // Returns 429 Too Many Requests
            ApiResponse<UploadResponse> errorResponse = ApiResponse.error("Upload limit exceeded! You can perform up to 5 uploads every 30 seconds.");
            return new ResponseEntity<>(errorResponse, HttpStatus.TOO_MANY_REQUESTS);
        }

        // 2. Malware & Signature Scan (FileSecurityScanner)
        fileSecurityScanner.scanFile(file);

        // 3. Structural Integrity & MIME spoof Validation (FileValidator)
        fileValidator.validate(file, type);

        // 4. Resolve User Profile
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("Uploader profile not found in system."));

        // 5. Store File utilizing active Storage Strategy
        String fileUrl = fileStorageService.storeFile(file, folder);

        // 6. Resolve Active Strategy Name
        String storageStrategy = fileUrl.startsWith("/uploads/") ? "LOCAL" : "S3";

        // 7. Track Upload in DB Telemetry
        UploadMetadata metadata = new UploadMetadata();
        metadata.setFileUrl(fileUrl);
        metadata.setOriginalFileName(file.getOriginalFilename());
        metadata.setContentType(file.getContentType());
        metadata.setFileSize(file.getSize());
        metadata.setStorageStrategy(storageStrategy);
        metadata.setUploadedBy(user);
        uploadMetadataRepository.save(metadata);

        // 8. Generate Optional Pre-signed URL for downloads (only if S3 is strategy)
        String presignedUrl = null;
        if ("S3".equals(storageStrategy)) {
            // Extract S3 key path (e.g. folder/filename.ext) from the full URL
            String s3Key = folder + "/" + fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            presignedUrl = fileStorageService.generatePresignedUrl(s3Key, 15); // 15 Minutes expiration
        } else {
            // Local fallback returns relative access path directly
            presignedUrl = fileStorageService.generatePresignedUrl(fileUrl, 15);
        }

        UploadResponse uploadResponse = new UploadResponse(
                fileUrl,
                file.getOriginalFilename(),
                file.getSize(),
                storageStrategy,
                presignedUrl
        );

        logger.info("Upload successfully processed. Target URL: {}", fileUrl);
        
        return ResponseEntity.ok(ApiResponse.success("File uploaded successfully.", uploadResponse));
    }
}

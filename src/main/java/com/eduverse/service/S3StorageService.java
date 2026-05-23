package com.eduverse.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * ============================================================================
 * AWS S3 STORAGE CONCRETE STRATEGY
 * ============================================================================
 * 
 * Manages all file operations directly on AWS S3 buckets using AWS Java SDK 2.x.
 * 
 * RESILIENCY tactics:
 * - **Thread-Safe Exponential Retries**: Upload operations are wrapped inside a 
 *   resilient retry loop that catches transient S3 anomalies and backs off exponentially.
 * - **Pre-signed URLs**: Supports secure, timed-access link generations.
 */
@Service("s3StorageService")
public class S3StorageService implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String region;

    public S3StorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.s3.bucket:eduverse-marketplace-bucket}") String bucketName,
            @Value("${aws.s3.region:us-east-1}") String region
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.region = region;
    }

    @Override
    public String storeFile(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Generate clean random UUID filename for safety
        String secureFileName = UUID.randomUUID().toString() + extension;
        String s3Key = folder + "/" + secureFileName;

        logger.info("Initializing cloud upload to S3 bucket '{}' with key '{}'", bucketName, s3Key);

        // Resilient Exponential Retry Configs
        int maxAttempts = 3;
        long initialBackoffMs = 500;
        double multiplier = 2.0;

        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // Build Put Object Request
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType(file.getContentType())
                        .build();

                // Stream file payload to S3
                s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
                
                logger.info("Cloud file successfully uploaded to S3 bucket. Key: {}", s3Key);
                
                // Return S3 public/access URL
                return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
            } catch (S3Exception | IOException ex) {
                lastException = ex;
                logger.warn("S3 upload attempt {} failed for key '{}': {}. Retrying...", attempt, s3Key, ex.getMessage());
                
                if (attempt < maxAttempts) {
                    long backoffMs = (long) (initialBackoffMs * Math.pow(multiplier, attempt - 1));
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Resilient upload retry interrupted.", e);
                    }
                }
            }
        }

        logger.error("All {} S3 upload attempts failed for key '{}'!", maxAttempts, s3Key);
        throw new RuntimeException("Failed to upload file to cloud storage after retries.", lastException);
    }

    @Override
    public void deleteFile(String fileUrl) {
        logger.info("Attempting cloud file deletion for URL: {}", fileUrl);

        if (fileUrl == null || !fileUrl.contains(".amazonaws.com/")) {
            logger.warn("Deletion bypassed: URL '{}' does not represent an S3 cloud path.", fileUrl);
            return;
        }

        // Extract S3 key path from URL
        String bucketUrlPrefix = String.format("s3.%s.amazonaws.com/", region);
        int index = fileUrl.indexOf(bucketUrlPrefix);
        
        if (index == -1) {
            // Check alternative regional S3 URL prefix format
            bucketUrlPrefix = String.format("%s.s3.%s.amazonaws.com/", bucketName, region);
            index = fileUrl.indexOf(bucketUrlPrefix);
            if (index == -1) {
                // Check general non-regional S3 URL prefix format
                bucketUrlPrefix = ".amazonaws.com/";
                index = fileUrl.indexOf(bucketUrlPrefix);
            }
        }

        if (index == -1) {
            logger.error("Could not parse S3 key from URL: {}", fileUrl);
            return;
        }

        String s3Key = fileUrl.substring(fileUrl.indexOf(bucketUrlPrefix) + bucketUrlPrefix.length());
        
        // Remove leading bucket name if present in parsed key (depends on regional path prefixing style)
        if (s3Key.startsWith(bucketName + "/")) {
            s3Key = s3Key.substring(bucketName.length() + 1);
        }

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            logger.info("Successfully deleted S3 cloud object with key: {}", s3Key);
        } catch (S3Exception ex) {
            logger.error("Failed to delete S3 cloud object key: '{}'", s3Key, ex);
        }
    }

    @Override
    public String generatePresignedUrl(String fileKey, int expirationInMinutes) {
        if (s3Presigner == null) {
            logger.warn("Pre-signed URL request bypassed: S3Presigner bean is unconfigured. Returning direct key.");
            return fileKey;
        }

        logger.debug("Generating pre-signed read URL for key '{}' expiring in {} mins", fileKey, expirationInMinutes);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationInMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();
            
            logger.info("Successfully generated pre-signed URL for key: {}", fileKey);
            return presignedUrl;
        } catch (Exception ex) {
            logger.error("Failed to generate pre-signed S3 URL for key: {}", fileKey, ex);
            return fileKey; // Fallback to direct key
        }
    }
}

package com.eduverse.config;

import com.eduverse.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * ============================================================================
 * AWS S3 STORAGE CONFIGURATION FACTORY
 * ============================================================================
 * 
 * Provides beans for managing cloud storage connections (S3Client, S3Presigner).
 * 
 * Implements resilient conditional fallback:
 * If 'aws.s3.enabled' is set to false, or the credentials are left as default 
 * developer placeholders ("YOUR_ACCESS_KEY"), this configuration gracefully 
 * registers LocalStorageService as the `@Primary` FileStorageService strategy.
 * 
 * This ensures zero-setup onboarding and clean offline operation for developers.
 */
@Configuration
public class S3Config {

    private static final Logger logger = LoggerFactory.getLogger(S3Config.class);

    @Value("${aws.accessKeyId:YOUR_ACCESS_KEY}")
    private String accessKeyId;

    @Value("${aws.secretKey:YOUR_SECRET_KEY}")
    private String secretKey;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

    /**
     * Builds S3Client bean. Uses placeholder credentials if cloud storage is unconfigured
     * to prevent spring boot application bootstrap from crashing.
     */
    @Bean
    public S3Client s3Client() {
        String activeKey = (accessKeyId == null || accessKeyId.equals("YOUR_ACCESS_KEY")) ? "dummy-key" : accessKeyId;
        String activeSecret = (secretKey == null || secretKey.equals("YOUR_SECRET_KEY")) ? "dummy-secret" : secretKey;

        logger.info("Initializing AWS S3 Client with region: {}", region);
        
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(activeKey, activeSecret)
                ))
                .build();
    }

    /**
     * Builds S3Presigner bean used to generate secure pre-signed file URLs.
     */
    @Bean
    public S3Presigner s3Presigner() {
        String activeKey = (accessKeyId == null || accessKeyId.equals("YOUR_ACCESS_KEY")) ? "dummy-key" : accessKeyId;
        String activeSecret = (secretKey == null || secretKey.equals("YOUR_SECRET_KEY")) ? "dummy-secret" : secretKey;
        
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(activeKey, activeSecret)
                ))
                .build();
    }

    /**
     * Strategy Selection:
     * Resolves the main FileStorageService strategy injected across the marketplace backend.
     */
    @Bean
    @Primary
    public FileStorageService fileStorageService(
            FileStorageService s3StorageService,
            FileStorageService localStorageService
    ) {
        boolean hasValidCredentials = accessKeyId != null 
                && !accessKeyId.isEmpty() 
                && !accessKeyId.equals("YOUR_ACCESS_KEY")
                && secretKey != null 
                && !secretKey.isEmpty() 
                && !secretKey.equals("YOUR_SECRET_KEY");

        if (s3Enabled && hasValidCredentials) {
            logger.info(">>> SUCCESS: Active S3 strategy selected as primary FileStorageService. Cloud uploads enabled!");
            return s3StorageService;
        } else {
            logger.info(">>> FALLBACK: Local fallback directory strategy selected as primary FileStorageService.");
            return localStorageService;
        }
    }
}

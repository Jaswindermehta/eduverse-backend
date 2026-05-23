package com.eduverse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ============================================================================
 * UPLOAD RESPONSE DTO
 * ============================================================================
 * 
 * Secure data transfer object encapsulating the results of a file upload.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {

    private String fileUrl;
    
    private String originalFileName;
    
    private Long fileSize;
    
    private String storageStrategy;
    
    // Optional pre-signed secure access URL (for S3 storage strategy)
    private String presignedUrl;
}

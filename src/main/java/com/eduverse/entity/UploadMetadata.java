package com.eduverse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * UPLOAD METADATA ENTITY
 * ============================================================================
 * 
 * Tracks telemetry records in the PostgreSQL database for successful file uploads.
 * Stores crucial information including file sizes, storage strategy used, and
 * the uploader's user account details.
 */
@Entity
@Table(name = "upload_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Fully qualified URL or file key to access the file
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    // The clean name of the file uploaded by the client
    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    // Resolved MIME Content-Type header
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    // File size in bytes
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    // The strategy used: e.g. "S3" or "LOCAL"
    @Column(name = "storage_strategy", nullable = false, length = 20)
    private String storageStrategy;

    // Link back to the user who uploaded the file
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    // Timestamps for audit logs
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

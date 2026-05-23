package com.eduverse.repository;

import com.eduverse.entity.UploadMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * ============================================================================
 * UPLOAD METADATA DATABASE REPOSITORY
 * ============================================================================
 * 
 * Maps all standard database operations for the "UploadMetadata" entity with a "Long" ID.
 * Automatically managed by Spring Data JPA.
 */
@Repository
public interface UploadMetadataRepository extends JpaRepository<UploadMetadata, Long> {
}

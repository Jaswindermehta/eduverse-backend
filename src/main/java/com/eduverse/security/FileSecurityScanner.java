package com.eduverse.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * PLACEHOLDER FILE SECURITY SCANNER ARCHITECTURE
 * ============================================================================
 * 
 * Simulates real-time security antivirus scanning (e.g. ClamAV integration) 
 * on raw file streams before storage occurs.
 * 
 * Demonstrates how to block malicious files, EICAR test signatures, or files
 * tagged as containing security exploits.
 */
@Component
public class FileSecurityScanner {

    private static final Logger logger = LoggerFactory.getLogger(FileSecurityScanner.class);

    // EICAR Standard Antivirus Test String signature (used by security systems to test malware scanning)
    private static final String EICAR_SIGNATURE = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";

    /**
     * Scans the incoming file byte stream for malware or virus signatures.
     * Throws SecurityException if a threat is discovered.
     */
    public void scanFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        logger.info("Initializing security malware scan on file: {}", filename);

        // 1. Simulating check by checking specific forbidden filenames
        if (filename != null && (filename.toLowerCase().contains("virus") || filename.toLowerCase().contains("exploit"))) {
            logger.error("Antivirus block: file '{}' matched dangerous naming signatures", filename);
            throw new SecurityException("Antivirus block: File contains dangerous content or exploits.");
        }

        // 2. Scan file stream for EICAR signature (simulate deep packet inspection)
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String content = reader.lines().collect(Collectors.joining("\n"));
            if (content.contains(EICAR_SIGNATURE)) {
                logger.error("CRITICAL: EICAR malware test signature discovered in file: {}", filename);
                throw new SecurityException("Antivirus block: File is infected with a known malware test signature.");
            }
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            // Ignore stream read errors to prevent blocking valid binary attachments (like zipped directories)
            logger.debug("Skipped deep text content stream inspection for binary file: {}", filename);
        }

        logger.info("Antivirus scan completed. Clean signature verified for file: {}", filename);
    }
}

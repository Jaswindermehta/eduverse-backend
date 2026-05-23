package com.eduverse.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================================
 * UPLOAD RATE LIMITER
 * ============================================================================
 * 
 * Implements a thread-safe Token-Bucket rate limiting algorithm per user session
 * to protect storage resources and disk allocation from denial-of-service abuse.
 * 
 * Configured limits:
 * - Maximum bucket size: 5 tokens (allows burst of up to 5 uploads).
 * - Refill rate: 1 token refilled every 30 seconds.
 */
@Component
public class UploadRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(UploadRateLimiter.class);

    private static final long BUCKET_CAPACITY = 5;
    private static final long REFILL_PERIOD_MS = 30000; // 30 seconds per token
    
    // Concurrent map storing token buckets per user identity
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Checks if the user is allowed to perform an upload.
     * Consumes one token if available. Returns false if bucket is empty.
     */
    public boolean allowUpload(String username) {
        TokenBucket bucket = buckets.computeIfAbsent(username, k -> new TokenBucket(BUCKET_CAPACITY));
        return bucket.tryConsume();
    }

    /**
     * Nested class modeling the Token-Bucket structure.
     */
    private static class TokenBucket {
        private final long capacity;
        private double tokens;
        private long lastRefillTime;

        public TokenBucket(long capacity) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            
            if (elapsed > 0) {
                // Calculate refilled tokens based on time elapsed
                double tokensToAdd = (double) elapsed / REFILL_PERIOD_MS;
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillTime = now;
            }
        }
    }
}

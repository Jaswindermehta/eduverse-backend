package com.eduverse.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================================
 * CUSTOM API RATE LIMITING FILTER (BUCKET4J)
 * ============================================================================
 * 
 * This filter intercept requests to sensitive resources (Authentication,
 * Uploads, and Notifications) and applies a strict Token Bucket algorithm to
 * prevent DDoS, brute-force, and resource-bloating spam upload attacks.
 * 
 * It identifies clients by their IP address (supporting standard proxy headers
 * like 'X-Forwarded-For') and tracks client limits using a thread-safe map.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // Thread-safe map storing a token bucket for each individual Client IP address
    private final ConcurrentHashMap<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    // The limit: 10 requests maximum capacity, refilling 1 token every 6 seconds (10 requests/minute)
    private final Bandwidth limit = Bandwidth.classic(10, Refill.intervally(1, Duration.ofSeconds(6)));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 1. Determine if the incoming request targets a rate-limited endpoint
        if (isRateLimitedPath(uri)) {
            String clientIp = getClientIp(request);
            
            // Fetch the client's existing bucket or create a brand new one
            Bucket bucket = ipBuckets.computeIfAbsent(clientIp, k -> Bucket.builder().addLimit(limit).build());

            // Try to consume exactly 1 token from the bucket
            if (!bucket.tryConsume(1)) {
                // If no tokens are left in the bucket, reject the request with HTTP 429
                handleRateLimitError(response);
                return;
            }
        }

        // Proceed to the next filter in the chain if within limits or path is un-limited
        filterChain.doFilter(request, response);
    }

    /**
     * Determines whether the requested URI is subject to rate-limiting protection.
     */
    private boolean isRateLimitedPath(String uri) {
        return uri.startsWith("/api/auth/") || 
               uri.startsWith("/api/uploads") || 
               uri.startsWith("/api/notifications");
    }

    /**
     * Helper to resolve the true client IP address. 
     * Inspects proxy headers (like X-Forwarded-For) first, falling back to direct socket remote IP.
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            // In case of multiple proxies, the true client IP is the very first entry
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Generates a clean, consistent JSON error payload matching the standard ApiResponse DTO format.
     */
    private void handleRateLimitError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String jsonPayload = "{"
                + "\"success\":false,"
                + "\"message\":\"Too many requests. Please try again later.\","
                + "\"data\":null"
                + "}";

        response.getWriter().write(jsonPayload);
        response.getWriter().flush();
    }
}

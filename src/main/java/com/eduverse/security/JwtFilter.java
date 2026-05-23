package com.eduverse.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * ============================================================================
 * JWT SECURITY REQUEST INTERCEPTOR FILTER
 * ============================================================================
 * 
 * This filter sits directly in Spring's Security Filter Chain. It intercepts
 * every incoming HTTP request before it reaches our controller endpoints.
 * 
 * It scans the HTTP headers for a "Authorization: Bearer <JWT_TOKEN>" layout.
 * If found, it validates the token stateless-ly using JwtService.
 * If the token is valid, it registers the user into Spring's security context,
 * effectively "logging them in" for the duration of this single request!
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    // Constructor Injection for maximum testability
    public JwtFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Intercepts HTTP request, extracts the JWT, and verifies security contexts.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Get the URI of the incoming request for debugging
        final String requestUri = request.getRequestURI();
        logger.debug("JwtFilter intercepting request for URI: {}", requestUri);

        // 1. Read the HTTP 'Authorization' header
        final String authHeader = request.getHeader("Authorization");
        final String jwtToken;
        final String userEmail;

        // 2. If the header is missing, or does not start with "Bearer ", 
        // skip this filter and let the request proceed to the next security checkpoint.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("No Bearer token found in headers for URI: {}. Proceeding with filter chain.", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract the clean JWT token (skip the first 7 characters: "Bearer ")
        jwtToken = authHeader.substring(7);
        
        try {
            // 4. Extract the user's email from the token payload
            userEmail = jwtService.extractEmail(jwtToken);
            logger.debug("Extracted user email '{}' from JWT", userEmail);

            // 5. If we found an email, and the user is NOT already logged in in the current thread context
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Load the UserDetails representation from our database
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // 6. Check if the token is valid and matches the database records
                if (jwtService.isTokenValid(jwtToken, userDetails.getUsername())) {
                    logger.debug("JWT Token is valid. Setting security authentication context for user: {}", userEmail);

                    // Build Spring Security's native login token carrier
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // We pass null as password because the user is already authenticated stateless-ly!
                            userDetails.getAuthorities() // Contains their roles (e.g. ROLE_STUDENT)
                    );

                    // Bind HTTP request details to the authentication object
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 7. Inject the authenticated token into Spring Security Context.
                    // From this moment onwards, Spring Security recognizes the user as "logged in"!
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    logger.warn("JWT Token validation failed for user: {}", userEmail);
                }
            }
        } catch (Exception e) {
            // Log security parsing failures without crashing the application
            logger.error("Failed to parse or validate JWT token in Filter: {}", e.getMessage());
        }

        // 8. Call the next filter in the security chain
        filterChain.doFilter(request, response);
    }
}

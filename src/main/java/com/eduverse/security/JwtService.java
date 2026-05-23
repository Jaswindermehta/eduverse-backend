package com.eduverse.security;

import com.eduverse.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * ============================================================================
 * JWT (JSON WEB TOKEN) UTILITY SERVICE
 * ============================================================================
 * 
 * This service is responsible for handling all JWT operations:
 * 1. Generating tokens when a user registers or logs in successfully.
 * 2. Extracting claims (like user email and roles) from the token on incoming calls.
 * 3. Checking if the token has expired or has been tampered with.
 * 
 * JWT is completely stateless. The server doesn't store tokens in memory;
 * it simply checks their cryptographic signatures on each request to identify
 * the caller!
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    // Read the secret key value defined in application.properties
    @Value("${eduverse.jwt.secret}")
    private String secretKey;

    // Read the token life span limit defined in application.properties (e.g. 24 hours)
    @Value("${eduverse.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Extracts the user email (which serves as the "Subject" of our token) from the JWT.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts a single specific claim from the JWT token.
     * We pass a resolver function to parse a specific field from the Claims block.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a new signed JWT token for a specific User.
     * We inject the user's specific role name as a custom claim so that the
     * security filter can read it easily without checking the database every time!
     * 
     * @param user The authenticated user.
     * @return A signed, secure JWT string.
     */
    public String generateToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        // Inject the user's security role as a claim (e.g. role="STUDENT")
        extraClaims.put("role", user.getRole().getRoleName().name());
        
        logger.debug("Generating new JWT token for user: {} with role: {}", user.getEmail(), user.getRole().getRoleName());
        return buildToken(extraClaims, user.getEmail(), jwtExpiration);
    }

    /**
     * Core helper method to build and sign the JWT.
     */
    private String buildToken(Map<String, Object> extraClaims, String email, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                // Sets the token expiration timestamp: (Current Time + Expiration Interval)
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                // Sign the token cryptographically using our HMAC-SHA256 signature algorithm
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates if a token is authentic and belongs to the active user.
     * 
     * @param token The JWT token extracted from the request headers.
     * @param email The loaded email of the user.
     * @return true if token is mathematically valid, not expired, and matches user.
     */
    public boolean isTokenValid(String token, String email) {
        try {
            final String extractedEmail = extractEmail(token);
            boolean emailsMatch = extractedEmail.equals(email);
            boolean isExpired = isTokenExpired(token);
            
            logger.debug("Validating token for email: {}. Emails match: {}, Token expired: {}", email, emailsMatch, isExpired);
            return (emailsMatch && !isExpired);
        } catch (Exception e) {
            logger.error("Token validation failed due to error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the token's expiration date has passed the current time.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date claim from the token.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Parses the cryptographic claims from the token payload.
     * This will fail (throwing an exception) if the token was tampered with or
     * signed with an incorrect key!
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Helper to decode our Base64 secret key and convert it into a secure signing Key object.
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

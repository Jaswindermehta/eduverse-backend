package com.eduverse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * USER DATA TRANSFER OBJECT (USER PROFILE DATA)
 * ============================================================================
 * 
 * This class is a "safe" version of our User entity. 
 * 
 * In a production-grade system, we NEVER return the actual User entity in our API
 * responses. If we did, we would accidentally send the encrypted password hash
 * over the network, which is a major security vulnerability!
 * 
 * UserDto only contains public information that is safe to display to the
 * client, such as full name, email, role, and registration date.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private Long id;
    
    private String fullName;
    
    private String email;
    
    private String role;
    
    private boolean enabled;
    
    private LocalDateTime createdAt;
}

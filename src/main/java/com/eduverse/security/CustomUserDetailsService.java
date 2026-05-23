package com.eduverse.security;

import com.eduverse.entity.User;
import com.eduverse.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Collections;

/**
 * ============================================================================
 * CUSTOM SPRING SECURITY USER DETAILS SERVICE
 * ============================================================================
 * 
 * This service is utilized by Spring Security's authentication manager to load
 * user credentials from our PostgreSQL database during authentication operations.
 * 
 * It implements UserDetailsService and converts our database "User" entity
 * into Spring Security's standard "UserDetails" interface.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    
    private final UserRepository userRepository;

    // Constructor Injection (NO field injection using @Autowired)
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Looks up the user in the database by their email. If the user is found, 
     * maps their details into a standard Spring Security UserDetails container.
     * 
     * @param username In our platform, the "username" refers to the user's "email".
     * @return UserDetails container carrying email, hashed password, and roles.
     * @throws UsernameNotFoundException If the email is not registered in our database.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading UserDetails from database for email: {}", username);
        
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> {
                    logger.warn("Authentication failed: User with email {} not found", username);
                    return new UsernameNotFoundException("User not found with email: " + username);
                });

        // Set up the user's security authority lists (e.g. ROLE_STUDENT)
        // Spring Security expects roles to be prefixed with "ROLE_" by default
        String roleNameWithPrefix = "ROLE_" + user.getRole().getRoleName().name();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleNameWithPrefix);

        logger.debug("UserDetails loaded successfully. Mapping email {} to authority {}", username, roleNameWithPrefix);

        // Return Spring Security's native User object carrying email, password, active status,
        // and authorities list
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(), // isEnabled
                true,             // accountNonExpired
                true,             // credentialsNonExpired
                true,             // accountNonLocked
                Collections.singletonList(authority) // User's single role authority
        );
    }
}

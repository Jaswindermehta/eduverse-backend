package com.eduverse.config;

import com.eduverse.security.CustomUserDetailsService;
import com.eduverse.security.JwtFilter;
import com.eduverse.security.RateLimitingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ============================================================================
 * SPRING SECURITY & JWT FILTER CHAIN CONFIGURATION
 * ============================================================================
 * 
 * This configuration class acts as the central security control panel of our
 * backend application. It defines who can access what endpoints, configures
 * the password encryption standard, sets the server session policies to stateless
 * mode, and wires in our custom JWT request interceptor filter.
 * 
 * We use modern Spring Security 6.x configurations:
 * - @EnableWebSecurity: Tells Spring to enable web security rules.
 * - @EnableMethodSecurity: Allows us to secure individual controller methods
 *   using annotations like @PreAuthorize("hasRole('ADMIN')") directly on methods.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;
    private final RateLimitingFilter rateLimitingFilter;

    // Constructor Injection
    public SecurityConfig(JwtFilter jwtFilter, CustomUserDetailsService userDetailsService, RateLimitingFilter rateLimitingFilter) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    /**
     * Define the PasswordEncoder bean. 
     * We use BCrypt Hashing, which is the gold standard for hashing database passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the AuthenticationProvider bean. 
     * We use DaoAuthenticationProvider which connects Spring Security to our CustomUserDetailsService
     * and utilizes BCryptPasswordEncoder to verify incoming login passwords.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Expose Spring's default AuthenticationManager as a bean.
     * This manager coordinates credentials validation.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Defines the primary SecurityFilterChain that intercepts and manages requests.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable Cross-Site Request Forgery (CSRF). Since we use stateless JWTs stored
            //    in clients (and not cookies), CSRF is not a security vector for our API.
            .csrf(AbstractHttpConfigurer::disable)
            
            // 2. Configure HTTP path permissions using modern lambda format.
            .authorizeHttpRequests(auth -> auth
                // Allow anyone to register or login publicly
                .requestMatchers("/api/auth/**").permitAll()
                
                // Allow public course and category browsing
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/courses/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/categories/**").permitAll()
                
                // Category creation (Admin Only)
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/categories").hasRole("ADMIN")
                
                // Course publishing and editing (Instructors & Admins)
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/courses").hasAnyRole("INSTRUCTOR", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/courses/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/courses/**").hasAnyRole("INSTRUCTOR", "ADMIN")
                
                // Student enrollments and reviews (Students & Admins)
                .requestMatchers("/api/courses/*/enroll").hasAnyRole("STUDENT", "ADMIN")
                .requestMatchers("/api/courses/*/reviews").hasAnyRole("STUDENT", "ADMIN")
                
                // Allow anyone to access the OpenAPI Swagger Documentation screens, view static uploads, or probe health stats
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/uploads/**",
                        "/actuator/**"
                ).permitAll()
                
                // Secure specific administrative paths (Admin Only)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // All other API endpoints require a valid login authentication token
                .anyRequest().authenticated()
            )
            
            // 3. Force stateless session management. Spring won't create any HTTP sessions,
            //    requiring clients to send their JWT in the Authorization header on every call.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 4. Register our Custom database Authentication Provider
            .authenticationProvider(authenticationProvider())
            
            // 5. Inject our custom JwtFilter BEFORE the default UsernamePasswordAuthenticationFilter.
            //    This checks the Bearer token and authenticates the caller first!
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            
            // 6. Inject our RateLimitingFilter BEFORE the JWT filter to catch spam uploads/logins early
            .addFilterBefore(rateLimitingFilter, JwtFilter.class);

        return http.build();
    }
}

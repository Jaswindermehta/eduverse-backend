package com.eduverse.service;

import com.eduverse.dto.AuthResponse;
import com.eduverse.dto.LoginRequest;
import com.eduverse.dto.RegisterRequest;
import com.eduverse.dto.UserDto;
import com.eduverse.entity.Role;
import com.eduverse.entity.RoleName;
import com.eduverse.entity.User;
import com.eduverse.exception.InvalidCredentialsException;
import com.eduverse.exception.ResourceNotFoundException;
import com.eduverse.mapper.UserMapper;
import com.eduverse.repository.RoleRepository;
import com.eduverse.repository.UserRepository;
import com.eduverse.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * AUTHENTICATION BUSINESS SERVICE LAYER
 * ============================================================================
 * 
 * This service contains the primary core business logic for user registration
 * and credential validation logins.
 * 
 * It manages duplicate checks, fetches security roles from PostgreSQL, hashes
 * plain-text passwords using BCrypt, and delegates token generation to JwtService.
 * 
 * We use @Transactional to ensure that database operations are committed fully
 * as a single unit or rolled back completely if any database error occurs.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    // Constructor Injection (Strictly avoiding field injection @Autowired)
    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    /**
     * Registers a new User into the platform database.
     * 
     * @param request Contains full name, email, password, and desired role.
     * @return AuthResponse containing token and user profile details.
     */
    @Transactional
    public AuthResponse registerUser(RegisterRequest request) {
        logger.info("Initiating user registration process for email: {}", request.getEmail());

        // 1. Double check that the email is not already taken
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Registration rejected: Email {} is already in use!", request.getEmail());
            throw new IllegalArgumentException("An account with this email address already exists! Please login instead.");
        }

        // 2. Map and resolve the requested role name
        String requestedRoleString = request.getRole().toUpperCase().trim();
        RoleName roleName;
        try {
            roleName = RoleName.valueOf(requestedRoleString);
        } catch (IllegalArgumentException e) {
            logger.warn("Registration rejected: Invalid role name input '{}'", request.getRole());
            throw new IllegalArgumentException("Invalid role provided. Role must be 'STUDENT' or 'INSTRUCTOR'!");
        }

        // 3. Prevent hackers from registering as system "ADMIN" via public APIs!
        if (roleName == RoleName.ADMIN) {
            logger.warn("Security Alert: Unauthorized attempt to register as ADMIN via registration endpoint for email: {}", request.getEmail());
            throw new IllegalArgumentException("Registration of ADMIN accounts is strictly prohibited via public APIs!");
        }

        // 4. Fetch the resolved Role entity from the PostgreSQL database
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> {
                    logger.error("Database seed error: Role entity '{}' not found in database", roleName);
                    return new ResourceNotFoundException("Role '" + roleName + "' could not be found. Please check database seeding.");
                });

        // 5. Encrypt the plain-text password using BCrypt
        String encryptedPassword = passwordEncoder.encode(request.getPassword());
        logger.debug("Successfully hashed plain password using BCrypt");

        // 6. Instantiate a new User entity and map values
        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPassword(encryptedPassword);
        user.setRole(role);
        user.setEnabled(true); // Active by default

        // 7. Commit the new user record to PostgreSQL
        User savedUser = userRepository.save(user);
        logger.info("User registered successfully. Assigned database ID: {}", savedUser.getId());

        // 8. Generate a stateless JWT token containing their email and role claims
        String token = jwtService.generateToken(savedUser);

        // 9. Map User entity to a safe UserDto to hide passwords
        UserDto userDto = userMapper.toDto(savedUser);

        // Return the final AuthResponse package
        return AuthResponse.builder()
                .token(token)
                .user(userDto)
                .build();
    }

    /**
     * Authenticates existing users using their email and password.
     * 
     * @param request Contains login email and plain-text password.
     * @return AuthResponse containing token and user details if successful.
     */
    @Transactional(readOnly = true)
    public AuthResponse loginUser(LoginRequest request) {
        String cleanEmail = request.getEmail().toLowerCase().trim();
        logger.info("Login attempt initiated for email: {}", cleanEmail);

        // 1. Look up the user record by email
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> {
                    logger.warn("Login failed: User with email {} does not exist", cleanEmail);
                    return new InvalidCredentialsException("Invalid email address or password. Please try again.");
                });

        // 2. Check if the account has been disabled
        if (!user.isEnabled()) {
            logger.warn("Login rejected: Account for email {} is suspended/disabled", cleanEmail);
            throw new InvalidCredentialsException("Your account has been deactivated. Please contact support.");
        }

        // 3. Compare hashed passwords using matches()
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Login failed: Password did not match hashed record for email: {}", cleanEmail);
            throw new InvalidCredentialsException("Invalid email address or password. Please try again.");
        }

        logger.info("Authentication successful. Issuing JWT for user: {}", cleanEmail);

        // 4. Issue a new signed JWT Token
        String token = jwtService.generateToken(user);

        // 5. Convert User entity to safe UserDto
        UserDto userDto = userMapper.toDto(user);

        // Return AuthResponse
        return AuthResponse.builder()
                .token(token)
                .user(userDto)
                .build();
    }
}

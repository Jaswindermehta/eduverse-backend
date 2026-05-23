package com.eduverse.config;

import com.eduverse.entity.*;
import com.eduverse.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * ============================================================================
 * DATABASE INITIALIZATION SEEDER
 * ============================================================================
 * 
 * Automatically executed once upon application startup.
 * Seeds standard Roles, default Test Accounts, Categories, and a demo Course
 * with complete content modules.
 */
@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    // Strict constructor dependency injection
    public DatabaseSeeder(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            CourseRepository courseRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        logger.info("Database Seeder check: verifying roles, test user accounts, categories, and courses...");

        // 1. Seed Roles
        Role studentRole = seedRole(RoleName.STUDENT);
        Role instructorRole = seedRole(RoleName.INSTRUCTOR);
        Role adminRole = seedRole(RoleName.ADMIN);

        // 2. Seed Test Users
        seedUser("System Admin", "admin@eduverse.com", "admin123", adminRole);
        User janeInstructor = seedUser("Jane Instructor", "instructor@eduverse.com", "instructor123", instructorRole);
        seedUser("John Student", "student@eduverse.com", "student123", studentRole);

        // 3. Seed Categories
        Category devCategory = seedCategory("Software Development", "Learn modern programming languages and web frameworks");
        seedCategory("Data Science", "Dive deep into AI, Machine Learning, Python, and SQL databases");
        seedCategory("Design", "Explore UI/UX fundamentals and graphical design paradigms");

        // 4. Seed a Demo Course
        if (janeInstructor != null && devCategory != null) {
            seedDemoCourse(janeInstructor, devCategory);
        }

        logger.info("Database Seeder completed successfully!");
    }

    /**
     * Seeds a role in the database if it doesn't already exist.
     */
    private Role seedRole(RoleName roleName) {
        Optional<Role> existingRole = roleRepository.findByRoleName(roleName);
        if (existingRole.isPresent()) {
            return existingRole.get();
        }

        logger.info("Seeding Role '{}' into database...", roleName);
        Role role = new Role();
        role.setRoleName(roleName);
        return roleRepository.save(role);
    }

    /**
     * Seeds a test user in the database if their email is not registered.
     */
    private User seedUser(String fullName, String email, String plainPassword, Role role) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        logger.info("Seeding test account: {} ({}) with role '{}'", fullName, email, role.getRoleName());
        
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setRole(role);
        user.setEnabled(true);

        return userRepository.save(user);
    }

    /**
     * Seeds a Category.
     */
    private Category seedCategory(String name, String description) {
        Optional<Category> existingCategory = categoryRepository.findByName(name);
        if (existingCategory.isPresent()) {
            return existingCategory.get();
        }

        logger.info("Seeding Category: '{}'", name);
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);

        return categoryRepository.save(category);
    }

    /**
     * Seeds a Demo Course with chapters/lectures.
     */
    private void seedDemoCourse(User instructor, Category category) {
        String courseTitle = "Spring Boot 3.x Development Masterclass";
        
        // Check if course already exists to avoid duplication on subsequent runs
        boolean courseExists = courseRepository.searchCourses(courseTitle, null, null, null, org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .anyMatch(c -> c.getTitle().equals(courseTitle));

        if (courseExists) {
            logger.debug("Demo Course '{}' already exists. Skipping seeding.", courseTitle);
            return;
        }

        logger.info("Seeding Demo Course: '{}' (Authored by: {})", courseTitle, instructor.getFullName());

        Course course = new Course();
        course.setTitle(courseTitle);
        course.setDescription("A complete guide to building production-grade, modular, and secure REST APIs utilizing Spring Boot 3.x, Spring Security 6.x, and JPA.");
        course.setThumbnailUrl("https://eduverse-assets.s3.amazonaws.com/thumbnails/spring-boot-3.png");
        course.setPrice(new BigDecimal("49.99"));
        course.setInstructor(instructor);
        course.setCategory(category);
        course.setActive(true);

        // Add Course lectures
        CourseContent module1 = new CourseContent();
        module1.setTitle("1. Course Overview and Environment Setup");
        module1.setContentUrl("https://eduverse-assets.s3.amazonaws.com/lessons/sb-lesson-1.mp4");
        module1.setSequenceOrder(1);
        course.addContent(module1);

        CourseContent module2 = new CourseContent();
        module2.setTitle("2. Creating REST API Gateways and Dependency Mappings");
        module2.setContentUrl("https://eduverse-assets.s3.amazonaws.com/lessons/sb-lesson-2.mp4");
        module2.setSequenceOrder(2);
        course.addContent(module2);

        CourseContent module3 = new CourseContent();
        module3.setTitle("3. JWT Security Architecture, Filters, and Database Seeders");
        module3.setContentUrl("https://eduverse-assets.s3.amazonaws.com/lessons/sb-lesson-3.mp4");
        module3.setSequenceOrder(3);
        course.addContent(module3);

        courseRepository.save(course);
        logger.info("Demo Course '{}' with 3 content modules successfully seeded!", courseTitle);
    }
}

package com.eduverse.repository;

import com.eduverse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * ============================================================================
 * USER DATABASE REPOSITORY
 * ============================================================================
 * 
 * Just like RoleRepository, this interface is handled by Spring Data JPA.
 * It maps all standard database operations for the "User" entity with a "Long" ID.
 * 
 * It automatically provides rich operations like user registration saves, 
 * loading profiles, deleting accounts, etc.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Look up a user in the database by their unique email address.
     * Used during Login and Security validation.
     * 
     * @param email The email address to look up.
     * @return An Optional containing the User if found, or empty if not.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user with the specified email address already exists.
     * Used during Registration to prevent duplicate accounts.
     * 
     * @param email The email address to check.
     * @return true if the email is already in use, false otherwise.
     */
    boolean existsByEmail(String email);
}

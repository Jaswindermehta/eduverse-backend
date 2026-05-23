package com.eduverse.repository;

import com.eduverse.entity.Role;
import com.eduverse.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * ============================================================================
 * ROLE DATABASE REPOSITORY
 * ============================================================================
 * 
 * This interface is managed by Spring Data JPA. When the application starts, 
 * Spring automatically implements this interface and generates all the database
 * code required to perform CRUD (Create, Read, Update, Delete) operations on
 * the "roles" table.
 * 
 * We extend JpaRepository<Role, Long> which tells Spring that:
 * 1. This repository is for the "Role" entity.
 * 2. The primary key of the Role entity is of type "Long".
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Finds a security role in the database by its name (STUDENT, INSTRUCTOR, ADMIN).
     * 
     * @param roleName The name of the role to search for.
     * @return An Optional containing the Role if found, or empty if not.
     */
    Optional<Role> findByRoleName(RoleName roleName);
}

package com.eduverse.repository;

import com.eduverse.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * ============================================================================
 * CATEGORY REPOSITORY
 * ============================================================================
 * 
 * Interacts directly with the 'categories' database schema.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    // Finds a category by its unique name
    Optional<Category> findByName(String name);

    // Checks if a category with a given name exists in the system
    boolean existsByName(String name);
}

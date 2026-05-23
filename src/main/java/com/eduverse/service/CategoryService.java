package com.eduverse.service;

import com.eduverse.dto.CategoryDto;
import com.eduverse.entity.Category;
import com.eduverse.exception.ResourceNotFoundException;
import com.eduverse.mapper.CourseMapper;
import com.eduverse.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

/**
 * ============================================================================
 * CATEGORY SERVICE
 * ============================================================================
 * 
 * Handles all business logic and validations associated with Course Categories.
 * 
 * DESIGN PRACTICES:
 * - Constructor Injection (NO @Autowired on fields).
 * - Centralized @Transactional boundaries.
 * - Beginner-friendly logging and exceptions.
 */
@Service
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final CourseMapper courseMapper;

    // Constructor Injection (strict SOLID design)
    public CategoryService(CategoryRepository categoryRepository, CourseMapper courseMapper) {
        this.categoryRepository = categoryRepository;
        this.courseMapper = courseMapper;
    }

    /**
     * Retrieves all categories from the database.
     */
    @Cacheable(value = "categories", key = "'all'")
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        logger.debug("Fetching all course categories from database...");
        return categoryRepository.findAll().stream()
                .map(courseMapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single category by its unique ID.
     */
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return courseMapper.toCategoryDto(category);
    }

    /**
     * Creates a new category (Admin Only boundary).
     */
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryDto createCategory(CategoryDto categoryDto) {
        logger.info("Attempting to create category: {}", categoryDto.getName());
        
        if (categoryRepository.existsByName(categoryDto.getName())) {
            throw new IllegalArgumentException("Category with name '" + categoryDto.getName() + "' already exists.");
        }

        Category category = new Category();
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());

        Category savedCategory = categoryRepository.save(category);
        logger.info("Category successfully created with ID: {}", savedCategory.getId());
        return courseMapper.toCategoryDto(savedCategory);
    }
}

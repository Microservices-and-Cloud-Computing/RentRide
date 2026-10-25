package com.rentride.vehicleservice.service.impl;

import com.rentride.vehicleservice.dto.CategoryCreateRequest;
import com.rentride.vehicleservice.dto.CategoryResponse;
import com.rentride.vehicleservice.entity.Category;
import com.rentride.vehicleservice.exception.DuplicateResourceException;
import com.rentride.vehicleservice.exception.ResourceNotFoundException;
import com.rentride.vehicleservice.repository.CategoryRepository;
import com.rentride.vehicleservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Production implementation of {@link CategoryService}.
 * Manages category retrieval, unique name verification, and persistence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryResponse> getAllCategories() {
        log.info("Fetching all vehicle categories");
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        log.info("Fetching category by ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        return mapToResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        String normalizedName = request.getName().trim().toUpperCase();
        log.info("Creating vehicle category: '{}'", normalizedName);

        if (categoryRepository.existsByName(normalizedName)) {
            throw new DuplicateResourceException("Category already exists with name: " + normalizedName);
        }

        Category category = Category.builder()
                .name(normalizedName)
                .description(request.getDescription())
                .baseDailyRate(request.getBaseDailyRate())
                .build();

        Category savedCategory = categoryRepository.save(category);
        log.info("Successfully created category: id={}, name='{}'", savedCategory.getId(), savedCategory.getName());
        return mapToResponse(savedCategory);
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .baseDailyRate(category.getBaseDailyRate())
                .build();
    }
}


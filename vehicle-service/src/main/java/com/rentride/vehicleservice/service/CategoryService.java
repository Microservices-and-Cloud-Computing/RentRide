package com.rentride.vehicleservice.service;

import com.rentride.vehicleservice.dto.CategoryCreateRequest;
import com.rentride.vehicleservice.dto.CategoryResponse;

import java.util.List;

/**
 * Service interface defining vehicle category classification and catalog operations.
 */
public interface CategoryService {

    /**
     * Retrieves all available vehicle categories.
     *
     * @return list of category response payloads
     */
    List<CategoryResponse> getAllCategories();

    /**
     * Retrieves a single category by its unique ID.
     *
     * @param id category unique identifier
     * @return category response details
     */
    CategoryResponse getCategoryById(Long id);

    /**
     * Creates and registers a new vehicle category.
     *
     * @param request category creation payload
     * @return persisted category response details
     */
    CategoryResponse createCategory(CategoryCreateRequest request);
}


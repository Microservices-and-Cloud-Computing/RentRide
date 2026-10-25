package com.rentride.vehicleservice.controller;

import com.rentride.vehicleservice.dto.CategoryCreateRequest;
import com.rentride.vehicleservice.dto.CategoryResponse;
import com.rentride.vehicleservice.dto.ErrorResponse;
import com.rentride.vehicleservice.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing endpoints for vehicle classification categories and baseline pricing.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Category Management", description = "Endpoints for vehicle classification categories, baseline pricing, and discovery")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Retrieves all vehicle categories in the platform.
     *
     * @return 200 OK with list of CategoryResponse
     */
    @GetMapping
    @Operation(summary = "List all vehicle categories", description = "Retrieves all vehicle classifications and their baseline daily rental rates")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of all vehicle categories",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryResponse.class))))
    })
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        log.info("REST GET /api/v1/categories");
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Retrieves a single category by its unique identifier.
     *
     * @param id category unique identifier
     * @return 200 OK with CategoryResponse
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieves specifications and baseline rates for a specific vehicle category")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Category not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable("id") Long id) {
        log.info("REST GET /api/v1/categories/{}", id);
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    /**
     * Registers a new vehicle category.
     *
     * @param request category creation payload
     * @return 201 Created with persisted CategoryResponse
     */
    @PostMapping
    @Operation(summary = "Create a vehicle category", description = "Registers a new vehicle classification category with baseline pricing")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created successfully",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed for category payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Category name already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        log.info("REST POST /api/v1/categories: name='{}'", request.getName());
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}


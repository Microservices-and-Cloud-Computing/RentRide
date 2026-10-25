package com.rentride.vehicleservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentride.vehicleservice.dto.CategoryCreateRequest;
import com.rentride.vehicleservice.dto.CategoryResponse;
import com.rentride.vehicleservice.exception.DuplicateResourceException;
import com.rentride.vehicleservice.exception.GlobalExceptionHandler;
import com.rentride.vehicleservice.exception.ResourceNotFoundException;
import com.rentride.vehicleservice.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WebMvc slice tests for {@link CategoryController}.
 * Verifies routing, JSON serialization, validation, and error handling.
 */
@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @DisplayName("GET /api/v1/categories: Returns 200 OK with list of categories")
    void testGetAllCategories_Success() throws Exception {
        List<CategoryResponse> categories = List.of(
                CategoryResponse.builder().id(1L).name("SEDAN").baseDailyRate(new BigDecimal("4500.00")).build(),
                CategoryResponse.builder().id(2L).name("SUV").baseDailyRate(new BigDecimal("7500.00")).build()
        );

        when(categoryService.getAllCategories()).thenReturn(categories);

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("SEDAN")))
                .andExpect(jsonPath("$[1].name", is("SUV")));
    }

    @Test
    @DisplayName("GET /api/v1/categories/{id}: Returns 200 OK when category exists")
    void testGetCategoryById_Success() throws Exception {
        CategoryResponse response = CategoryResponse.builder()
                .id(1L)
                .name("SEDAN")
                .description("Economy Sedan")
                .baseDailyRate(new BigDecimal("4500.00"))
                .build();

        when(categoryService.getCategoryById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("SEDAN")));
    }

    @Test
    @DisplayName("GET /api/v1/categories/{id}: Returns 404 Not Found when category does not exist")
    void testGetCategoryById_NotFound() throws Exception {
        when(categoryService.getCategoryById(999L))
                .thenThrow(new ResourceNotFoundException("Category not found with ID: 999"));

        mockMvc.perform(get("/api/v1/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Category not found")));
    }

    @Test
    @DisplayName("POST /api/v1/categories: Returns 201 Created when category is valid")
    void testCreateCategory_Success() throws Exception {
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .name("LUXURY")
                .description("Luxury vehicles")
                .baseDailyRate(new BigDecimal("15000.00"))
                .build();

        CategoryResponse response = CategoryResponse.builder()
                .id(3L)
                .name("LUXURY")
                .description("Luxury vehicles")
                .baseDailyRate(new BigDecimal("15000.00"))
                .build();

        when(categoryService.createCategory(any(CategoryCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.name", is("LUXURY")));
    }

    @Test
    @DisplayName("POST /api/v1/categories: Returns 400 Bad Request when validation fails")
    void testCreateCategory_ValidationFailure() throws Exception {
        CategoryCreateRequest invalidRequest = CategoryCreateRequest.builder()
                .name("") // blank
                .baseDailyRate(new BigDecimal("100.00")) // below 500.00
                .build();

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.errors.name", notNullValue()))
                .andExpect(jsonPath("$.errors.baseDailyRate", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/categories: Returns 409 Conflict when category name already exists")
    void testCreateCategory_DuplicateName() throws Exception {
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .name("SEDAN")
                .baseDailyRate(new BigDecimal("5000.00"))
                .build();

        when(categoryService.createCategory(any(CategoryCreateRequest.class)))
                .thenThrow(new DuplicateResourceException("Category already exists with name: SEDAN"));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }
}


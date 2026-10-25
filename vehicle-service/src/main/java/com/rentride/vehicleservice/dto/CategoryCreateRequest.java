package com.rentride.vehicleservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request payload for creating and registering a new vehicle category.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for creating a new vehicle category")
public class CategoryCreateRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    @Schema(description = "Category classification name (e.g., SEDAN, SUV, LUXURY)", example = "LUXURY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Detailed category description", example = "Premium luxury executive vehicles with top-tier comfort")
    private String description;

    @NotNull(message = "Base daily rate is required")
    @DecimalMin(value = "500.00", message = "Base daily rate cannot be less than 500.00")
    @Schema(description = "Baseline daily rental rate in LKR", example = "15000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal baseDailyRate;
}


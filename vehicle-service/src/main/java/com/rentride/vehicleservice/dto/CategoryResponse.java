package com.rentride.vehicleservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response payload representing a vehicle category classification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Vehicle category details response payload")
public class CategoryResponse {

    @Schema(description = "Unique category ID", example = "1")
    private Long id;

    @Schema(description = "Category classification name", example = "SEDAN")
    private String name;

    @Schema(description = "Category description and intended use cases", example = "Economy Sedan — Fuel-efficient 4-door vehicles")
    private String description;

    @Schema(description = "Baseline daily rental rate in LKR", example = "4500.00")
    private BigDecimal baseDailyRate;
}

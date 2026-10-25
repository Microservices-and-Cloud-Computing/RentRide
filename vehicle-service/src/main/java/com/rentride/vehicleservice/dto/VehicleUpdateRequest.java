package com.rentride.vehicleservice.dto;

import com.rentride.vehicleservice.entity.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request payload for updating an existing vehicle listing's pricing, availability, and metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for updating an existing vehicle's rate, location, seating, or operational status")
public class VehicleUpdateRequest {

    @NotNull(message = "Daily rental rate is required")
    @DecimalMin(value = "500.00", message = "Daily rate cannot be less than 500.00")
    @Schema(description = "Updated daily rental rate in LKR (minimum 500.00)", example = "9000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal dailyRentalRate;

    @NotBlank(message = "Vehicle location is required")
    @Size(min = 2, max = 50, message = "Location must be between 2 and 50 characters")
    @Schema(description = "Updated operational or pickup depot city", example = "Colombo", requiredMode = Schema.RequiredMode.REQUIRED)
    private String location;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    @Schema(description = "Updated public URL to vehicle image", example = "https://images.rentride.com/vehicles/prius-2021-updated.jpg")
    private String imageUrl;

    @NotNull(message = "Seating capacity is required")
    @Min(value = 1, message = "Seating capacity must be at least 1")
    @Max(value = 60, message = "Seating capacity cannot exceed 60")
    @Schema(description = "Updated passenger seating capacity (1-60)", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer seatingCapacity;

    @NotNull(message = "Vehicle status is required")
    @Schema(description = "Updated operational lifecycle status", example = "AVAILABLE", requiredMode = Schema.RequiredMode.REQUIRED)
    private VehicleStatus status;
}

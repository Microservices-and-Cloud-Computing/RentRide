package com.rentride.vehicleservice.dto;

import com.rentride.vehicleservice.entity.VehicleStatus;
import jakarta.validation.constraints.DecimalMin;
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
public class VehicleUpdateRequest {

    @NotNull(message = "Daily rental rate is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Daily rental rate must be non-negative")
    private BigDecimal dailyRentalRate;

    @NotBlank(message = "Vehicle location is required")
    @Size(max = 150, message = "Location cannot exceed 150 characters")
    private String location;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;

    @NotNull(message = "Seating capacity is required")
    @Min(value = 1, message = "Seating capacity must be at least 1")
    private Integer seatingCapacity;

    @NotNull(message = "Vehicle status is required")
    private VehicleStatus status;
}

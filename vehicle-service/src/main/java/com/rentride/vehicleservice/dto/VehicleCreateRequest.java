package com.rentride.vehicleservice.dto;

import com.rentride.vehicleservice.entity.FuelType;
import com.rentride.vehicleservice.entity.TransmissionType;
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
 * Request payload for creating and registering a new vehicle in a rental provider's fleet.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleCreateRequest {

    @NotBlank(message = "Vehicle make is required")
    @Size(max = 100, message = "Vehicle make cannot exceed 100 characters")
    private String make;

    @NotBlank(message = "Vehicle model is required")
    @Size(max = 100, message = "Vehicle model cannot exceed 100 characters")
    private String model;

    @NotNull(message = "Manufacturing year is required")
    @Min(value = 2000, message = "Manufacturing year must not be earlier than 2000")
    @Max(value = 2027, message = "Manufacturing year must not exceed 2027")
    private Integer manufacturingYear;

    @NotBlank(message = "License plate number is required")
    @Size(max = 50, message = "License plate number cannot exceed 50 characters")
    private String licensePlateNumber;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Seating capacity is required")
    @Min(value = 1, message = "Seating capacity must be at least 1")
    private Integer seatingCapacity;

    @NotNull(message = "Fuel type is required")
    private FuelType fuelType;

    @NotNull(message = "Transmission type is required")
    private TransmissionType transmission;

    @NotNull(message = "Daily rental rate is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Daily rental rate must be non-negative")
    private BigDecimal dailyRentalRate;

    @NotBlank(message = "Vehicle location is required")
    @Size(max = 150, message = "Location cannot exceed 150 characters")
    private String location;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;
}

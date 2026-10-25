package com.rentride.vehicleservice.dto;

import com.rentride.vehicleservice.entity.FuelType;
import com.rentride.vehicleservice.entity.TransmissionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
@Schema(description = "Request payload for onboarding a new vehicle into the provider fleet")
public class VehicleCreateRequest {

    @NotBlank(message = "Vehicle make is required")
    @Size(max = 100, message = "Vehicle make cannot exceed 100 characters")
    @Schema(description = "Vehicle manufacturer or brand", example = "Toyota", requiredMode = Schema.RequiredMode.REQUIRED)
    private String make;

    @NotBlank(message = "Vehicle model is required")
    @Size(max = 100, message = "Vehicle model cannot exceed 100 characters")
    @Schema(description = "Vehicle model name", example = "Prius", requiredMode = Schema.RequiredMode.REQUIRED)
    private String model;

    @NotNull(message = "Manufacturing year is required")
    @Min(value = 2000, message = "Year must be 2000 or newer")
    @Max(value = 2027, message = "Manufacturing year must not exceed 2027")
    @Schema(description = "Year of vehicle manufacture (2000 or newer)", example = "2021", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer manufacturingYear;

    @NotBlank(message = "License plate number is required")
    @Pattern(
            regexp = "^(?:[A-Z]{2,3}-)?[A-Z]{1,3}-[0-9]{4}$|^[0-9]{2,3}-[0-9]{4}$",
            message = "Invalid Sri Lankan vehicle registration plate format (e.g., WP-CBA-1234 or 65-4321)"
    )
    @Schema(description = "Sri Lankan vehicle registration license plate number", example = "WP-CBA-1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String licensePlateNumber;

    @NotNull(message = "Category ID is required")
    @Schema(description = "Unique identifier of the vehicle category classification", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryId;

    @NotNull(message = "Seating capacity is required")
    @Min(value = 1, message = "Seating capacity must be at least 1")
    @Max(value = 60, message = "Seating capacity cannot exceed 60")
    @Schema(description = "Maximum passenger seating capacity (1-60)", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer seatingCapacity;

    @NotNull(message = "Fuel type is required")
    @Schema(description = "Vehicle fuel consumption mechanism", example = "HYBRID", requiredMode = Schema.RequiredMode.REQUIRED)
    private FuelType fuelType;

    @NotNull(message = "Transmission type is required")
    @Schema(description = "Vehicle drivetrain transmission type", example = "AUTOMATIC", requiredMode = Schema.RequiredMode.REQUIRED)
    private TransmissionType transmission;

    @NotNull(message = "Daily rental rate is required")
    @DecimalMin(value = "500.00", message = "Daily rate cannot be less than 500.00")
    @Schema(description = "Daily rental rate in LKR (minimum 500.00)", example = "8500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal dailyRentalRate;

    @NotBlank(message = "Vehicle location is required")
    @Size(min = 2, max = 50, message = "Location must be between 2 and 50 characters")
    @Schema(description = "Operational or pickup depot city", example = "Colombo", requiredMode = Schema.RequiredMode.REQUIRED)
    private String location;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    @Schema(description = "Public URL to vehicle thumbnail or photo", example = "https://images.rentride.com/vehicles/prius-2021.jpg")
    private String imageUrl;
}

package com.rentride.vehicleservice.dto;

import com.rentride.vehicleservice.entity.FuelType;
import com.rentride.vehicleservice.entity.TransmissionType;
import com.rentride.vehicleservice.entity.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload representing full vehicle details for client display and catalog browsing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Full vehicle details response payload")
public class VehicleResponse {

    @Schema(description = "Unique identifier of the vehicle", example = "1")
    private Long id;

    @Schema(description = "ID of the rental provider owning the vehicle", example = "10")
    private Long providerId;

    @Schema(description = "Vehicle make or manufacturer", example = "Toyota")
    private String make;

    @Schema(description = "Vehicle model name", example = "Prius")
    private String model;

    @Schema(description = "Manufacturing year", example = "2021")
    private Integer manufacturingYear;

    @Schema(description = "License plate registration number", example = "WP-CBA-1234")
    private String licensePlateNumber;

    @Schema(description = "Vehicle category name", example = "SEDAN")
    private String categoryName;

    @Schema(description = "Vehicle category ID", example = "1")
    private Long categoryId;

    @Schema(description = "Passenger seating capacity", example = "5")
    private Integer seatingCapacity;

    @Schema(description = "Fuel type", example = "HYBRID")
    private FuelType fuelType;

    @Schema(description = "Transmission system", example = "AUTOMATIC")
    private TransmissionType transmission;

    @Schema(description = "Daily rental rate in LKR", example = "8500.00")
    private BigDecimal dailyRentalRate;

    @Schema(description = "Current operational lifecycle status", example = "AVAILABLE")
    private VehicleStatus status;

    @Schema(description = "Depot or pickup city", example = "Colombo")
    private String location;

    @Schema(description = "URL to vehicle photo", example = "https://images.rentride.com/vehicles/prius-2021.jpg")
    private String imageUrl;

    @Schema(description = "Timestamp when the vehicle was onboarded", example = "2026-10-25T10:00:00")
    private LocalDateTime createdAt;
}

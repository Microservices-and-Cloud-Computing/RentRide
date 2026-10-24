package com.rentride.vehicleservice.dto;

import com.rentride.vehicleservice.entity.FuelType;
import com.rentride.vehicleservice.entity.TransmissionType;
import com.rentride.vehicleservice.entity.VehicleStatus;
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
public class VehicleResponse {

    private Long id;
    private Long providerId;
    private String make;
    private String model;
    private Integer manufacturingYear;
    private String licensePlateNumber;
    private String categoryName;
    private Long categoryId;
    private Integer seatingCapacity;
    private FuelType fuelType;
    private TransmissionType transmission;
    private BigDecimal dailyRentalRate;
    private VehicleStatus status;
    private String location;
    private String imageUrl;
    private LocalDateTime createdAt;
}

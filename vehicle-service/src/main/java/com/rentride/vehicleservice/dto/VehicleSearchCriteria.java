package com.rentride.vehicleservice.dto;

import com.rentride.vehicleservice.entity.FuelType;
import com.rentride.vehicleservice.entity.TransmissionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Encapsulates dynamic vehicle search and filtering criteria provided by customers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Criteria parameters for dynamic vehicle inventory search and catalog discovery")
public class VehicleSearchCriteria {

    @Schema(description = "Filter by category ID", example = "1")
    private Long categoryId;

    @Schema(description = "Filter by vehicle city or depot location keyword (case-insensitive substring)", example = "Colombo")
    private String location;

    @Schema(description = "Filter by minimum daily rental rate in LKR", example = "5000.00")
    private BigDecimal minPrice;

    @Schema(description = "Filter by maximum daily rental rate in LKR", example = "15000.00")
    private BigDecimal maxPrice;

    @Schema(description = "Filter by vehicle fuel type", example = "PETROL")
    private FuelType fuelType;

    @Schema(description = "Filter by transmission type", example = "AUTOMATIC")
    private TransmissionType transmission;

    @Schema(description = "Filter by minimum seating capacity required", example = "4")
    private Integer minSeats;
}

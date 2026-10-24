package com.rentride.vehicleservice.dto;

import com.rentride.vehicleservice.entity.FuelType;
import com.rentride.vehicleservice.entity.TransmissionType;
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
public class VehicleSearchCriteria {

    private Long categoryId;
    private String location;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private FuelType fuelType;
    private TransmissionType transmission;
    private Integer minSeats;
}

package com.rentride.bookingservice.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object representing vehicle specifications retrieved from vehicle-service via Feign RPC.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class VehicleResponseDto {

    private Long id;
    private Long providerId;
    private String make;
    private String model;
    private BigDecimal dailyRentalRate;
    private String status;
    private String location;
    private String licensePlateNumber;
    private Integer seatingCapacity;
    private String categoryName;
}


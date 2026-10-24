package com.rentride.vehicleservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentride.vehicleservice.entity.VehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight Data Transfer Object providing vehicle availability status and pricing.
 *
 * <p>Designed for fast pre-booking checks by the Booking Service or edge clients
 * without loading heavy vehicle relationship structures.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleAvailabilityResponse {

    private Long vehicleId;

    private VehicleStatus status;

    @JsonProperty("isAvailable")
    private boolean isAvailable;

    private BigDecimal dailyRentalRate;

    @JsonProperty("isAvailable")
    public boolean isAvailable() {
        return isAvailable;
    }

    @JsonProperty("isAvailable")
    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }
}


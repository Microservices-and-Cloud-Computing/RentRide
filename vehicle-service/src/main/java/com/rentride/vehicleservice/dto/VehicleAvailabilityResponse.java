package com.rentride.vehicleservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentride.vehicleservice.entity.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Lightweight vehicle availability and rental rate response")
public class VehicleAvailabilityResponse {

    @Schema(description = "Unique identifier of the vehicle", example = "1")
    private Long vehicleId;

    @Schema(description = "Current operational status of the vehicle", example = "AVAILABLE")
    private VehicleStatus status;

    @JsonProperty("isAvailable")
    @Schema(description = "Whether the vehicle is eligible for reservation and booking", example = "true")
    private boolean isAvailable;

    @Schema(description = "Current daily rental rate in LKR", example = "8500.00")
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

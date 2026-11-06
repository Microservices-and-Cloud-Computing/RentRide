package com.rentride.bookingservice.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight Data Transfer Object capturing vehicle availability and rate for pre-booking validation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class VehicleAvailabilityDto {

    private Long vehicleId;
    private String status;

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


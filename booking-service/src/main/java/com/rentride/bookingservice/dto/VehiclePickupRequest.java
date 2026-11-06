package com.rentride.bookingservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for recording vehicle handover/pickup at the beginning of a rental.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiclePickupRequest {

    /**
     * Vehicle odometer reading in kilometers at time of customer pickup.
     */
    @NotNull(message = "Odometer reading is required at pickup")
    @Positive(message = "Odometer reading must be a positive number")
    private Double odometerReading;

    /**
     * Fuel tank level percentage (0 to 100) recorded during handover.
     */
    @NotNull(message = "Fuel level percentage is required at pickup")
    @Min(value = 0, message = "Fuel level cannot be less than 0%")
    @Max(value = 100, message = "Fuel level cannot exceed 100%")
    private Integer fuelLevelPercent;

    /**
     * Optional handover notes (e.g., existing pre-rental scratches, spare tire check).
     */
    @Size(max = 500, message = "Handover notes cannot exceed 500 characters")
    private String handoverNotes;
}


package com.rentride.bookingservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload for recording vehicle return inspection, odometer, fuel level, and condition.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleReturnRequest {

    /**
     * Vehicle odometer reading in kilometers upon return.
     */
    @NotNull(message = "Odometer reading is required at return")
    @Positive(message = "Odometer reading must be a positive number")
    private Double odometerReading;

    /**
     * Remaining fuel tank percentage (0 to 100) upon return inspection.
     */
    @NotNull(message = "Fuel level percentage is required at return")
    @Min(value = 0, message = "Fuel level cannot be less than 0%")
    @Max(value = 100, message = "Fuel level cannot exceed 100%")
    private Integer fuelLevelPercent;

    /**
     * Physical and mechanical condition of the vehicle evaluated during return inspection.
     */
    @NotNull(message = "Vehicle condition assessment is required")
    private ReturnCondition vehicleCondition;

    /**
     * Operational return remarks (e.g., fuel shortage, cleaning notes, damage descriptions).
     */
    @Size(max = 500, message = "Return notes cannot exceed 500 characters")
    private String returnNotes;

    /**
     * Additional fees assessed upon return (e.g., fuel penalty, late return fee, damage deposit deduction).
     */
    @Builder.Default
    @DecimalMin(value = "0.00", message = "Additional charges cannot be negative")
    private BigDecimal additionalCharges = BigDecimal.ZERO;
}


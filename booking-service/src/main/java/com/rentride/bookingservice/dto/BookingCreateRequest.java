package com.rentride.bookingservice.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request payload for creating a new vehicle reservation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCreateRequest {

    /**
     * Unique identifier of the vehicle to be reserved.
     */
    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    /**
     * Reservation start date (inclusive). Must be today or a future date.
     */
    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    /**
     * Reservation end date (inclusive). Must be equal to or after start date.
     */
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    /**
     * Optional customer notes or special requests for vehicle pickup.
     */
    @Size(max = 500, message = "Pickup notes cannot exceed 500 characters")
    private String pickupNotes;

    /**
     * Custom cross-field validation verifying end date does not precede start date.
     *
     * @return true if valid or if dates are null (deferring to @NotNull)
     */
    @AssertTrue(message = "End date must be equal to or after start date")
    public boolean isEndDateValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }
}


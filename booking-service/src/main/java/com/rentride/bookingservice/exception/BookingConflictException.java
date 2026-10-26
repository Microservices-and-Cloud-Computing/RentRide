package com.rentride.bookingservice.exception;

import java.time.LocalDate;

/**
 * Thrown when a new booking request collides with an existing active reservation
 * for the same vehicle over the specified date range.
 */
public class BookingConflictException extends RuntimeException {

    public BookingConflictException(String message) {
        super(message);
    }

    public BookingConflictException(Long vehicleId, LocalDate startDate, LocalDate endDate) {
        super(String.format("Vehicle ID %d already has an active reservation overlapping with requested dates [%s to %s]",
                vehicleId, startDate, endDate));
    }
}


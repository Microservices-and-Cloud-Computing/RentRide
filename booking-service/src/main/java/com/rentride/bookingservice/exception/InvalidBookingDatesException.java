package com.rentride.bookingservice.exception;

/**
 * Thrown when booking dates violate business rules (e.g., end date before start date,
 * start date in the past, or rental window exceeding maximum allowed duration).
 */
public class InvalidBookingDatesException extends RuntimeException {

    public InvalidBookingDatesException(String message) {
        super(message);
    }
}


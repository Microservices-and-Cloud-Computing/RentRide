package com.rentride.paymentservice.exception;

/**
 * Exception thrown when a referenced booking cannot be found in booking-service.
 */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(String message) {
        super(message);
    }

    public BookingNotFoundException(Long bookingId) {
        super("Booking not found with ID: " + bookingId);
    }
}


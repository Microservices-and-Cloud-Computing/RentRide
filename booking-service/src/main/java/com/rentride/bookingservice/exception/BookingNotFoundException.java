package com.rentride.bookingservice.exception;

/**
 * Thrown when a requested booking reservation cannot be found in the database.
 */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(String message) {
        super(message);
    }

    public BookingNotFoundException(Long id) {
        super("Booking reservation not found with ID: " + id);
    }
}


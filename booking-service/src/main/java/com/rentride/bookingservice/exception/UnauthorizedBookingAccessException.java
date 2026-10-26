package com.rentride.bookingservice.exception;

/**
 * Thrown when an authenticated user attempts to access a booking that does not belong
 * to them and they lack administrative privileges.
 */
public class UnauthorizedBookingAccessException extends RuntimeException {

    public UnauthorizedBookingAccessException(String message) {
        super(message);
    }
}


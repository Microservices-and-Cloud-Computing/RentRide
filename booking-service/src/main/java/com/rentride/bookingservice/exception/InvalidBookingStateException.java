package com.rentride.bookingservice.exception;

/**
 * Thrown when a lifecycle transition (such as vehicle pickup or return) cannot be performed
 * because the booking is not in the required state.
 */
public class InvalidBookingStateException extends RuntimeException {

    public InvalidBookingStateException(String message) {
        super(message);
    }
}


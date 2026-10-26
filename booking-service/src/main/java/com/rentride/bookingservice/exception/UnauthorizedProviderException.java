package com.rentride.bookingservice.exception;

/**
 * Thrown when an authenticated provider attempts to manage a vehicle handover or return
 * for a reservation that does not belong to their fleet.
 */
public class UnauthorizedProviderException extends RuntimeException {

    public UnauthorizedProviderException(String message) {
        super(message);
    }
}


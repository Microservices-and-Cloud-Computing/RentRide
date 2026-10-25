package com.rentride.bookingservice.exception;

/**
 * Exception thrown when a vehicle is unavailable for booking (e.g. status is RENTED or MAINTENANCE).
 */
public class VehicleNotAvailableException extends RuntimeException {

    public VehicleNotAvailableException(String message) {
        super(message);
    }
}


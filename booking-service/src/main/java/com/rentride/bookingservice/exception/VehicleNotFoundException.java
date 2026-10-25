package com.rentride.bookingservice.exception;

/**
 * Exception thrown when a vehicle referenced by ID cannot be found in vehicle-service.
 */
public class VehicleNotFoundException extends RuntimeException {

    public VehicleNotFoundException(String message) {
        super(message);
    }
}


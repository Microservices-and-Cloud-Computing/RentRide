package com.rentride.vehicleservice.exception;

/**
 * Exception thrown when attempting to create a vehicle with a duplicate unique attribute (such as license plate number).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}

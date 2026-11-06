package com.rentride.vehicleservice.exception;

/**
 * Exception thrown when a requested vehicle or category resource does not exist.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

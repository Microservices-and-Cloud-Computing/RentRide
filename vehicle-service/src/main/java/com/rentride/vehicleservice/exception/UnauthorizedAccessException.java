package com.rentride.vehicleservice.exception;

/**
 * Exception thrown when a user or provider attempts an operation on a resource they do not own or have permission to access.
 */
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }
}

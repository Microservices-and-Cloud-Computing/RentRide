package com.rentride.vehicleservice.exception;

/**
 * Exception thrown when an invalid or unauthorized vehicle operational status
 * transition is attempted, violating RentRide platform lifecycle rules.
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}


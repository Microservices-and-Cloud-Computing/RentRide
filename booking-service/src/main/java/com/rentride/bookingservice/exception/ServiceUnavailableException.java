package com.rentride.bookingservice.exception;

/**
 * Exception thrown when an external microservice (e.g., vehicle-service) is temporarily unreachable or times out.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}


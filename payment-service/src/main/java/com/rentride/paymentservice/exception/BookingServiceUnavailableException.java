package com.rentride.paymentservice.exception;

/**
 * Exception thrown when the remote booking-service is unavailable or times out during RPC invocation.
 */
public class BookingServiceUnavailableException extends RuntimeException {

    public BookingServiceUnavailableException(String message) {
        super(message);
    }

    public BookingServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}


package com.rentride.paymentservice.exception;

/**
 * Exception thrown when a payment attempt conflicts with existing state
 * (e.g., booking has already been paid and settled).
 */
public class PaymentConflictException extends RuntimeException {

    public PaymentConflictException(String message) {
        super(message);
    }
}


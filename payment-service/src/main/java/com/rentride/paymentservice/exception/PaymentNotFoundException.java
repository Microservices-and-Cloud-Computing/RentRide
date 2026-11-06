package com.rentride.paymentservice.exception;

/**
 * Exception thrown when a payment or transaction cannot be located.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }
}


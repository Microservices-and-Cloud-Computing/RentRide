package com.rentride.paymentservice.exception;

/**
 * Exception thrown when payment parameters or card details are invalid.
 */
public class InvalidPaymentDetailsException extends RuntimeException {

    public InvalidPaymentDetailsException(String message) {
        super(message);
    }
}


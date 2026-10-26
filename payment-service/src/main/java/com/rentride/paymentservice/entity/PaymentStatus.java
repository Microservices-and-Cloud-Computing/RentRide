package com.rentride.paymentservice.entity;

/**
 * Enumeration representing the lifecycle states of a payment transaction in RentRide platform.
 */
public enum PaymentStatus {

    /**
     * Payment has been initiated but not yet authorized or completed by the gateway.
     */
    PENDING,

    /**
     * Payment has been successfully authorized and settled.
     */
    SUCCESS,

    /**
     * Payment attempt was declined, expired, or failed gateway authorization.
     */
    FAILED,

    /**
     * Settled payment was fully or partially refunded to the customer.
     */
    REFUNDED
}


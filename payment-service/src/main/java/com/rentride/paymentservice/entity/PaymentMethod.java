package com.rentride.paymentservice.entity;

/**
 * Enumeration representing supported payment methods across the RentRide platform.
 */
public enum PaymentMethod {

    /**
     * Credit card payment (Visa, Mastercard, Amex, etc.).
     */
    CREDIT_CARD,

    /**
     * Debit card payment.
     */
    DEBIT_CARD,

    /**
     * Direct bank transfer / wire / LankaPay.
     */
    BANK_TRANSFER,

    /**
     * Digital wallet payment (Apple Pay, Google Pay, eZ Cash, etc.).
     */
    DIGITAL_WALLET
}


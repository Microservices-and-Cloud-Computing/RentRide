package com.rentride.paymentservice.service;

import com.rentride.paymentservice.dto.PaymentProcessRequest;
import com.rentride.paymentservice.dto.PaymentResponse;
import com.rentride.paymentservice.dto.TransactionVerifyResponse;

/**
 * Service interface for payment execution, idempotency enforcement, and verification.
 */
public interface PaymentService {

    /**
     * Executes payment checkout for a reservation.
     *
     * @param customerId ID of the authenticated customer initiating payment
     * @param request    payment parameters and method details
     * @return {@link PaymentResponse} with authorization status and transaction reference
     */
    PaymentResponse processPayment(Long customerId, PaymentProcessRequest request);

    /**
     * Verifies the authenticity and status of a transaction reference.
     *
     * @param transactionReference business reference code
     * @return {@link TransactionVerifyResponse} with verification result and timestamp
     */
    TransactionVerifyResponse verifyTransaction(String transactionReference);

    /**
     * Retrieves the latest payment attempt record for a given reservation.
     *
     * @param bookingId vehicle reservation identifier
     * @return {@link PaymentResponse} of the latest transaction attempt
     */
    PaymentResponse getPaymentByBookingId(Long bookingId);
}


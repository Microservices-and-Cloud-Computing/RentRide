package com.rentride.paymentservice.service;

import com.rentride.paymentservice.dto.PaymentProcessRequest;
import com.rentride.paymentservice.dto.PaymentResponse;
import com.rentride.paymentservice.dto.TransactionVerifyResponse;
import com.rentride.paymentservice.entity.Currency;
import com.rentride.paymentservice.entity.Payment;
import com.rentride.paymentservice.entity.PaymentStatus;
import com.rentride.paymentservice.exception.InvalidPaymentDetailsException;
import com.rentride.paymentservice.exception.PaymentConflictException;
import com.rentride.paymentservice.exception.PaymentNotFoundException;
import com.rentride.paymentservice.repository.PaymentRepository;
import com.rentride.paymentservice.service.gateway.GatewayResult;
import com.rentride.paymentservice.service.gateway.PaymentGatewaySimulator;
import com.rentride.paymentservice.util.TransactionReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Production implementation of {@link PaymentService} with idempotency checks,
 * gateway simulation, audit trails, and transactional consistency.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewaySimulator paymentGatewaySimulator;
    private final TransactionReferenceGenerator transactionReferenceGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse processPayment(Long customerId, PaymentProcessRequest request) {
        Long effectiveCustomerId = customerId != null ? customerId : request.getCustomerId();
        if (effectiveCustomerId == null) {
            throw new InvalidPaymentDetailsException("Customer ID is required to process payment");
        }

        log.info("Initiating checkout: bookingId={}, customerId={}, amount={} {}, method={}",
                request.getBookingId(),
                effectiveCustomerId,
                request.getAmount(),
                request.getCurrency(),
                request.getPaymentMethod());

        // 1. Idempotency Check: block duplicate payments for already completed bookings
        if (paymentRepository.existsByBookingIdAndStatus(request.getBookingId(), PaymentStatus.SUCCESS)) {
            log.warn("Payment conflict: bookingId={} already has a SUCCESS payment", request.getBookingId());
            throw new PaymentConflictException("Booking has already been paid and confirmed");
        }

        // 2. Generate unique alphanumeric transaction reference
        String transactionReference = transactionReferenceGenerator.generate();

        // 3. Persist initial payment record in PENDING state
        Payment payment = Payment.builder()
                .transactionReference(transactionReference)
                .bookingId(request.getBookingId())
                .customerId(effectiveCustomerId)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : Currency.LKR)
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Persisted initial PENDING transaction: id={}, txnRef={}", payment.getId(), transactionReference);

        // 4. Invoke simulated payment gateway
        GatewayResult gatewayResult = paymentGatewaySimulator.process(transactionReference, request);

        // 5. Update transaction state based on gateway response
        if (gatewayResult.isSuccessful()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setGatewayTransactionId(gatewayResult.getGatewayTransactionId());
            payment.setReceiptUrl(gatewayResult.getReceiptUrl());
            log.info("Payment settled successfully: txnRef={}, gatewayTxnId={}",
                    transactionReference, gatewayResult.getGatewayTransactionId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(gatewayResult.getFailureReason());
            log.warn("Payment failed: txnRef={}, reason={}", transactionReference, gatewayResult.getFailureReason());
        }

        Payment updatedPayment = paymentRepository.save(payment);
        return PaymentResponse.fromEntity(updatedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionVerifyResponse verifyTransaction(String transactionReference) {
        log.info("Verifying transaction reference: {}", transactionReference);

        Payment payment = paymentRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment transaction not found for reference: " + transactionReference));

        boolean isVerified = (payment.getStatus() == PaymentStatus.SUCCESS);
        LocalDateTime timestamp = payment.getUpdatedAt() != null ? payment.getUpdatedAt() : payment.getCreatedAt();

        return TransactionVerifyResponse.builder()
                .transactionReference(payment.getTransactionReference())
                .bookingId(payment.getBookingId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .isVerified(isVerified)
                .timestamp(timestamp)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        log.info("Fetching payment history for bookingId: {}", bookingId);

        List<Payment> payments = paymentRepository.findByBookingId(bookingId);
        if (payments.isEmpty()) {
            throw new PaymentNotFoundException("No payment records found for booking ID: " + bookingId);
        }

        // Return latest payment attempt
        Payment latest = payments.stream()
                .max(Comparator.comparing(Payment::getId))
                .orElseThrow(() -> new PaymentNotFoundException("No payment records found for booking ID: " + bookingId));

        return PaymentResponse.fromEntity(latest);
    }
}


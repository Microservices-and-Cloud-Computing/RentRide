package com.rentride.paymentservice.service;

import com.rentride.paymentservice.client.BookingServiceClient;
import com.rentride.paymentservice.dto.PaymentProcessRequest;
import com.rentride.paymentservice.dto.PaymentResponse;
import com.rentride.paymentservice.dto.TransactionVerifyResponse;
import com.rentride.paymentservice.dto.external.BookingPaymentCallbackRequest;
import com.rentride.paymentservice.dto.external.BookingSummaryResponse;
import com.rentride.paymentservice.entity.Currency;
import com.rentride.paymentservice.entity.Payment;
import com.rentride.paymentservice.entity.PaymentStatus;
import com.rentride.paymentservice.exception.BookingNotFoundException;
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

/**
 * Service implementation managing checkout payments, simulated gateway authorizations,
 * idempotency conflict enforcement, and OpenFeign callbacks to booking-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewaySimulator paymentGatewaySimulator;
    private final TransactionReferenceGenerator transactionReferenceGenerator;
    private final BookingServiceClient bookingServiceClient;

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

        // 2. Pre-check with Booking-Service via OpenFeign
        log.info("Verifying booking pre-conditions via OpenFeign for bookingId: {}", request.getBookingId());
        BookingSummaryResponse booking = bookingServiceClient.getBookingById(request.getBookingId());

        if (booking == null) {
            log.error("Booking verification failed: booking-service returned null for bookingId={}", request.getBookingId());
            throw new BookingNotFoundException(request.getBookingId());
        }

        // Verify reservation state is PENDING_PAYMENT before initiating gateway charges
        if (booking.getStatus() != null && !"PENDING_PAYMENT".equalsIgnoreCase(booking.getStatus())) {
            log.warn("Booking pre-check rejected: bookingId={}, status={}", request.getBookingId(), booking.getStatus());
            throw new IllegalStateException(String.format(
                    "Booking %s cannot be paid: current status is %s (must be PENDING_PAYMENT)",
                    booking.getBookingReference() != null ? booking.getBookingReference() : request.getBookingId(),
                    booking.getStatus()));
        }

        // Verify total cost matches requested amount
        if (booking.getTotalCost() != null && booking.getTotalCost().compareTo(request.getAmount()) != 0) {
            log.warn("Booking pre-check amount mismatch: requested={}, bookingCost={}", request.getAmount(), booking.getTotalCost());
            throw new InvalidPaymentDetailsException(String.format(
                    "Payment amount %s does not match booking total cost %s",
                    request.getAmount(), booking.getTotalCost()));
        }

        // 3. Generate unique alphanumeric transaction reference
        String transactionReference = transactionReferenceGenerator.generate();

        // 4. Persist initial payment record in PENDING state
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

        // 5. Invoke simulated payment gateway
        GatewayResult gatewayResult = paymentGatewaySimulator.process(transactionReference, request);

        // 6. Update transaction state and invoke Booking-Service callback
        if (gatewayResult.isSuccessful()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setGatewayTransactionId(gatewayResult.getGatewayTransactionId());
            payment.setReceiptUrl(gatewayResult.getReceiptUrl());
            log.info("Payment settled successfully: txnRef={}, gatewayTxnId={}",
                    transactionReference, gatewayResult.getGatewayTransactionId());

            // OpenFeign callback to Booking-Service
            try {
                log.info("Invoking OpenFeign callback to update booking-service status to CONFIRMED: bookingId={}, txnRef={}",
                        request.getBookingId(), transactionReference);

                BookingPaymentCallbackRequest callbackRequest = BookingPaymentCallbackRequest.builder()
                        .transactionReference(transactionReference)
                        .paymentStatus(PaymentStatus.SUCCESS)
                        .paidAmount(request.getAmount())
                        .paymentDate(LocalDateTime.now())
                        .build();

                bookingServiceClient.updatePaymentStatus(request.getBookingId(), callbackRequest);
                log.info("Booking-service successfully confirmed reservation for bookingId: {}", request.getBookingId());
            } catch (Exception ex) {
                log.error("CRITICAL COMPENSATION ALERT: Payment was settled (txnRef={}), but booking-service callback failed! Marking payment as REQUIRES_RECONCILIATION. Error: {}",
                        transactionReference, ex.getMessage(), ex);
                payment.setStatus(PaymentStatus.REQUIRES_RECONCILIATION);
                payment.setFailureReason("Booking status update callback failed: " + ex.getMessage());
            }
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(gatewayResult.getFailureReason());
            log.warn("Payment failed: txnRef={}, reason={}", transactionReference, gatewayResult.getFailureReason());

            // Notify booking-service of payment failure
            try {
                BookingPaymentCallbackRequest callbackRequest = BookingPaymentCallbackRequest.builder()
                        .transactionReference(transactionReference)
                        .paymentStatus(PaymentStatus.FAILED)
                        .paidAmount(request.getAmount())
                        .paymentDate(LocalDateTime.now())
                        .build();

                bookingServiceClient.updatePaymentStatus(request.getBookingId(), callbackRequest);
            } catch (Exception ex) {
                log.warn("Could not notify booking-service of payment failure for bookingId {}: {}",
                        request.getBookingId(), ex.getMessage());
            }
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

        return paymentRepository.findByBookingId(bookingId)
                .stream()
                .max((p1, p2) -> {
                    LocalDateTime t1 = p1.getCreatedAt() != null ? p1.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime t2 = p2.getCreatedAt() != null ? p2.getCreatedAt() : LocalDateTime.MIN;
                    int cmp = t1.compareTo(t2);
                    if (cmp != 0) {
                        return cmp;
                    }
                    Long id1 = p1.getId() != null ? p1.getId() : 0L;
                    Long id2 = p2.getId() != null ? p2.getId() : 0L;
                    return id1.compareTo(id2);
                })
                .map(PaymentResponse::fromEntity)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No payment records found for booking ID: " + bookingId));
    }
}

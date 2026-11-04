package com.rentride.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain event emitted by the Payment-Service after a payment transaction
 * completes — either successfully or with a failure.
 *
 * <p>Consumed from {@code notification.payment.processed.queue} via RabbitMQ.
 * The notification-service dispatches:
 * <ul>
 *   <li>A payment receipt email when {@code status == "SUCCESS"}.</li>
 *   <li>A payment failure alert when {@code status == "FAILED"}, prompting retry.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {

    /** Unique payment gateway transaction reference. */
    private String transactionReference;

    /** Associated booking reference. */
    private String bookingReference;

    /** Email address of the paying customer. */
    private String customerEmail;

    /** Display name of the customer. */
    private String customerName;

    /** Transaction amount. */
    private BigDecimal amount;

    /** ISO-4217 currency code (e.g., {@code LKR}, {@code USD}). */
    private String currency;

    /**
     * Payment method used (e.g., {@code CREDIT_CARD}, {@code DEBIT_CARD},
     * {@code BANK_TRANSFER}).
     */
    private String paymentMethod;

    /**
     * Final transaction status.
     * <ul>
     *   <li>{@code SUCCESS} — funds captured; triggers receipt email.</li>
     *   <li>{@code FAILED} — transaction declined; triggers failure-alert email.</li>
     * </ul>
     */
    private String status;

    /**
     * Human-readable reason for payment failure.
     * {@code null} when {@code status == "SUCCESS"}.
     */
    private String failureReason;

    /**
     * URL to the downloadable PDF receipt.
     * {@code null} when {@code status == "FAILED"}.
     */
    private String receiptUrl;

    /** Wall-clock timestamp when the payment result event was raised. */
    private LocalDateTime occurredOn;
}


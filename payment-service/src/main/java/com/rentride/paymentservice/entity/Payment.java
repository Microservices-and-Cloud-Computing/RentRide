package com.rentride.paymentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entity representing a financial payment transaction within the RentRide platform.
 *
 * <p>Tracks online checkout records, gateway responses, idempotent transaction references,
 * settlement status, and receipt links for vehicle rental bookings.
 */
@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_txn_ref", columnList = "transaction_reference", unique = true),
                @Index(name = "idx_payments_booking_id", columnList = "booking_id"),
                @Index(name = "idx_payments_customer_id", columnList = "customer_id"),
                @Index(name = "idx_payments_gateway_txn_id", columnList = "gateway_transaction_id"),
                @Index(name = "idx_payments_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    /**
     * Primary key identifier for the payment record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique alphanumeric business transaction reference (e.g., TXN-RR-20261026-XXXXXX).
     * Used for external tracking and idempotent payment retries.
     */
    @Column(name = "transaction_reference", nullable = false, unique = true, length = 64)
    private String transactionReference;

    /**
     * Foreign identifier referencing the reservation in booking-service.
     */
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    /**
     * Foreign identifier referencing the paying customer in user-service.
     */
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /**
     * Total financial monetary charge for the transaction.
     */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Monetary ISO currency of the transaction.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private Currency currency = Currency.LKR;

    /**
     * Payment method selected to execute the transaction.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    /**
     * Current lifecycle status of the payment.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * External gateway reference or mock transaction identifier returned by payment provider.
     */
    @Column(name = "gateway_transaction_id", length = 128)
    private String gatewayTransactionId;

    /**
     * Human-readable error message or gateway decline reason if status is FAILED.
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * Hosted URL or downloadable PDF receipt link for customer proof of payment.
     */
    @Column(name = "receipt_url", length = 512)
    private String receiptUrl;
}


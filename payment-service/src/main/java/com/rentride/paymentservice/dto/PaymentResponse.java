package com.rentride.paymentservice.dto;

import com.rentride.paymentservice.entity.Currency;
import com.rentride.paymentservice.entity.Payment;
import com.rentride.paymentservice.entity.PaymentMethod;
import com.rentride.paymentservice.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing the outcome of a payment operation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "PaymentResponse",
        description = "Response payload returned after a payment checkout or payment lookup operation"
)
public class PaymentResponse {

    @Schema(
            description = "Internal payment record identifier",
            example = "1"
    )
    private Long id;

    @Schema(
            description = "Unique business-level transaction reference generated for this payment attempt",
            example = "TXN-20241025-A1B2C3"
    )
    private String transactionReference;

    @Schema(
            description = "Booking reservation ID that this payment is associated with",
            example = "101"
    )
    private Long bookingId;

    @Schema(
            description = "Customer (user) ID who initiated the payment",
            example = "1"
    )
    private Long customerId;

    @Schema(
            description = "Transaction amount charged to the payment method",
            example = "17000.00"
    )
    private BigDecimal amount;

    @Schema(
            description = "ISO 4217 currency code for the transaction",
            example = "LKR",
            allowableValues = {"LKR", "USD"}
    )
    private Currency currency;

    @Schema(
            description = "Payment channel used for this transaction",
            example = "CREDIT_CARD",
            allowableValues = {"CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER", "DIGITAL_WALLET"}
    )
    private PaymentMethod paymentMethod;

    @Schema(
            description = "Final outcome of the payment — PENDING, COMPLETED, FAILED, or REFUNDED",
            example = "COMPLETED",
            allowableValues = {"PENDING", "COMPLETED", "FAILED", "REFUNDED"}
    )
    private PaymentStatus status;

    @Schema(
            description = "Transaction ID returned by the simulated card-payment gateway",
            example = "GW-9988776655"
    )
    private String gatewayTransactionId;

    @Schema(
            description = "URL to the payment receipt document (available for COMPLETED transactions only)",
            example = "http://localhost:8084/receipts/TXN-20241025-A1B2C3"
    )
    private String receiptUrl;

    @Schema(
            description = "Human-readable reason for failure (null when status is COMPLETED)",
            example = "Insufficient funds — card declined by gateway",
            nullable = true
    )
    private String failureReason;

    @Schema(
            description = "UTC timestamp when the payment record was persisted",
            example = "2024-10-25T14:30:00"
    )
    private LocalDateTime createdAt;

    /**
     * Factory method mapping a {@link Payment} JPA entity to a {@link PaymentResponse} DTO.
     *
     * @param payment entity to convert
     * @return populated response DTO
     */
    public static PaymentResponse fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }
        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionReference(payment.getTransactionReference())
                .bookingId(payment.getBookingId())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .receiptUrl(payment.getReceiptUrl())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}

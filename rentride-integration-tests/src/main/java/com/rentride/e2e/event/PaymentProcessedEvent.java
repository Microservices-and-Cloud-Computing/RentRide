package com.rentride.e2e.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Shared domain event emitted after a payment checkout transaction is processed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {

    private String transactionReference;
    private String bookingReference;
    private String customerEmail;
    private String customerName;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String status;
    private String failureReason;
    private String receiptUrl;
    private LocalDateTime occurredOn;
}


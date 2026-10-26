package com.rentride.paymentservice.dto;

import com.rentride.paymentservice.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for public and inter-service transaction verification queries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionVerifyResponse {

    private String transactionReference;
    private Long bookingId;
    private PaymentStatus status;
    private BigDecimal amount;
    private Boolean isVerified;
    private LocalDateTime timestamp;
}


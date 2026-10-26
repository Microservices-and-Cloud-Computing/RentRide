package com.rentride.bookingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing an RPC payment settlement callback payload from payment-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingPaymentCallbackRequest {

    @NotBlank(message = "Transaction reference is required")
    private String transactionReference;

    @NotNull(message = "Payment status is required")
    private String paymentStatus;

    @NotNull(message = "Paid amount is required")
    @Positive(message = "Paid amount must be positive")
    private BigDecimal paidAmount;

    private LocalDateTime paymentDate;
}


package com.rentride.paymentservice.dto.external;

import com.rentride.paymentservice.entity.PaymentStatus;
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
 * DTO representing an RPC payment status update callback sent from payment-service to booking-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingPaymentCallbackRequest {

    @NotBlank(message = "Transaction reference is required")
    private String transactionReference;

    @NotNull(message = "Payment status is required")
    private PaymentStatus paymentStatus;

    @NotNull(message = "Paid amount is required")
    @Positive(message = "Paid amount must be positive")
    private BigDecimal paidAmount;

    private LocalDateTime paymentDate;
}


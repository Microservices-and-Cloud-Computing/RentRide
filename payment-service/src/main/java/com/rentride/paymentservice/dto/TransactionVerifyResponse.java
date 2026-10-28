package com.rentride.paymentservice.dto;

import com.rentride.paymentservice.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(
        name = "TransactionVerifyResponse",
        description = "Verification report for a specific transaction reference — " +
                      "used by the booking-service callback and the public verify endpoint"
)
public class TransactionVerifyResponse {

    @Schema(
            description = "Unique business-level transaction reference being verified",
            example = "TXN-20241025-A1B2C3"
    )
    private String transactionReference;

    @Schema(
            description = "Booking reservation ID associated with this transaction",
            example = "101"
    )
    private Long bookingId;

    @Schema(
            description = "Final payment status at the time of verification",
            example = "COMPLETED",
            allowableValues = {"PENDING", "COMPLETED", "FAILED", "REFUNDED"}
    )
    private PaymentStatus status;

    @Schema(
            description = "Transaction amount that was charged",
            example = "17000.00"
    )
    private BigDecimal amount;

    @Schema(
            description = "True when the transaction status is COMPLETED and the payment was successfully captured; " +
                          "false for FAILED, PENDING, or REFUNDED states",
            example = "true"
    )
    private Boolean isVerified;

    @Schema(
            description = "UTC timestamp when the payment record was last evaluated",
            example = "2024-10-25T14:30:00"
    )
    private LocalDateTime timestamp;
}

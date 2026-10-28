package com.rentride.paymentservice.dto;

import com.rentride.paymentservice.entity.Currency;
import com.rentride.paymentservice.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object for initiating an online checkout / payment attempt.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "PaymentProcessRequest",
        description = "Request payload to initiate a payment checkout for a vehicle rental reservation"
)
public class PaymentProcessRequest {

    /**
     * Unique identifier of the booking reservation to be paid.
     */
    @NotNull(message = "Booking ID is required")
    @Schema(
            description = "Unique identifier of the booking reservation to be paid",
            example = "101",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long bookingId;

    /**
     * Optional customer ID (can be supplied in payload or via X-User-Id header).
     */
    @Schema(
            description = "Optional customer ID. If omitted, the value is taken from the X-User-Id request header " +
                          "injected by the API Gateway",
            example = "1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Long customerId;

    /**
     * Monetary transaction charge.
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100.00", message = "Minimum transaction amount is 100.00")
    @Schema(
            description = "Total rental cost matching the reservation amount (minimum 100.00)",
            example = "17000.00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal amount;

    /**
     * Transaction currency (default: LKR).
     */
    @Builder.Default
    @Schema(
            description = "ISO 4217 transaction currency code",
            example = "LKR",
            allowableValues = {"LKR", "USD"},
            defaultValue = "LKR",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Currency currency = Currency.LKR;

    /**
     * Selected payment channel / method.
     */
    @NotNull(message = "Payment method is required")
    @Schema(
            description = "Payment channel to use for this transaction",
            example = "CREDIT_CARD",
            allowableValues = {"CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER", "DIGITAL_WALLET"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private PaymentMethod paymentMethod;

    /**
     * 16-digit primary account number (PAN).
     */
    @Pattern(regexp = "^[0-9]{16}$", message = "Invalid card number format")
    @Schema(
            description = "16-digit card number (PAN). Use 4111111111114242 for an approved result; " +
                          "use 4111111111110000 for a declined/insufficient-funds result",
            example = "4111111111114242",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String cardNumber;

    /**
     * Expiry month (1-12).
     */
    @Min(value = 1, message = "Expiry month must be between 1 and 12")
    @Max(value = 12, message = "Expiry month must be between 1 and 12")
    @Schema(
            description = "Card expiry month (1–12)",
            example = "12",
            minimum = "1",
            maximum = "12",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Integer expiryMonth;

    /**
     * Expiry year (must be current or future year).
     */
    @Min(value = 2026, message = "Expiry year must be 2026 or later")
    @Schema(
            description = "Card expiry year (2026 or later)",
            example = "2028",
            minimum = "2026",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Integer expiryYear;

    /**
     * 3 or 4-digit card verification value.
     */
    @Pattern(regexp = "^[0-9]{3,4}$", message = "Invalid CVV")
    @Schema(
            description = "3 or 4-digit card security code printed on the card",
            example = "123",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String cvv;

    /**
     * Validates that card details are fully supplied if paymentMethod is a card.
     */
    @AssertTrue(message = "Card details (cardNumber, expiryMonth, expiryYear, cvv) are required for card payments")
    public boolean isCardDetailsSupplied() {
        if (paymentMethod == PaymentMethod.CREDIT_CARD || paymentMethod == PaymentMethod.DEBIT_CARD) {
            return cardNumber != null && !cardNumber.isBlank()
                    && expiryMonth != null
                    && expiryYear != null
                    && cvv != null && !cvv.isBlank();
        }
        return true;
    }

    /**
     * Returns a PCI-DSS compliant masked representation of the card number.
     *
     * @return masked card string (e.g. "**** **** **** 1234")
     */
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }
}

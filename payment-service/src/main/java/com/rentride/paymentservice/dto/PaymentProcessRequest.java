package com.rentride.paymentservice.dto;

import com.rentride.paymentservice.entity.Currency;
import com.rentride.paymentservice.entity.PaymentMethod;
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
public class PaymentProcessRequest {

    /**
     * Unique identifier of the booking reservation to be paid.
     */
    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    /**
     * Optional customer ID (can be supplied in payload or via X-User-Id header).
     */
    private Long customerId;

    /**
     * Monetary transaction charge.
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100.00", message = "Minimum transaction amount is 100.00")
    private BigDecimal amount;

    /**
     * Transaction currency (default: LKR).
     */
    @Builder.Default
    private Currency currency = Currency.LKR;

    /**
     * Selected payment channel / method.
     */
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    /**
     * 16-digit primary account number (PAN).
     */
    @Pattern(regexp = "^[0-9]{16}$", message = "Invalid card number format")
    private String cardNumber;

    /**
     * Expiry month (1-12).
     */
    @Min(value = 1, message = "Expiry month must be between 1 and 12")
    @Max(value = 12, message = "Expiry month must be between 1 and 12")
    private Integer expiryMonth;

    /**
     * Expiry year (must be current or future year).
     */
    @Min(value = 2026, message = "Expiry year must be 2026 or later")
    private Integer expiryYear;

    /**
     * 3 or 4-digit card verification value.
     */
    @Pattern(regexp = "^[0-9]{3,4}$", message = "Invalid CVV")
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


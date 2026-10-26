package com.rentride.paymentservice.service.gateway;

import com.rentride.paymentservice.dto.PaymentProcessRequest;
import com.rentride.paymentservice.entity.Currency;
import com.rentride.paymentservice.entity.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentGatewaySimulatorTest {

    private PaymentGatewaySimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new PaymentGatewaySimulator();
    }

    @Test
    @DisplayName("process: Card ending in 0000 should simulate insufficient funds decline")
    void testProcess_CardEndingIn0000_DeclinesInsufficientFunds() {
        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(1L)
                .amount(new BigDecimal("5000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111111111110000")
                .expiryMonth(12)
                .expiryYear(2027)
                .cvv("123")
                .build();

        GatewayResult result = simulator.process("TXN-RR-TEST-0000", request);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(PaymentGatewaySimulator.DECLINED_REASON_INSUFFICIENT_FUNDS);
        assertThat(result.getGatewayTransactionId()).isNull();
    }

    @Test
    @DisplayName("process: Card ending in 9999 should simulate gateway timeout")
    void testProcess_CardEndingIn9999_SimulatesTimeout() {
        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(1L)
                .amount(new BigDecimal("5000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .cardNumber("4111111111119999")
                .expiryMonth(5)
                .expiryYear(2028)
                .cvv("456")
                .build();

        GatewayResult result = simulator.process("TXN-RR-TEST-9999", request);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo(PaymentGatewaySimulator.DECLINED_REASON_GATEWAY_TIMEOUT);
    }

    @Test
    @DisplayName("process: Standard valid card should succeed with gateway transaction ID and receipt URL")
    void testProcess_ValidCard_ApprovesPayment() {
        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(2L)
                .amount(new BigDecimal("12500.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111111111111234")
                .expiryMonth(10)
                .expiryYear(2029)
                .cvv("789")
                .build();

        GatewayResult result = simulator.process("TXN-RR-TEST-OK", request);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getGatewayTransactionId()).startsWith("GW-TXN-");
        assertThat(result.getReceiptUrl()).isEqualTo("https://receipts.rentride.com/receipts/TXN-RR-TEST-OK.pdf");
        assertThat(result.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("process: Alternative payment channels (Digital Wallet, Bank Transfer) should approve successfully")
    void testProcess_DigitalWallet_ApprovesPayment() {
        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(3L)
                .amount(new BigDecimal("3500.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.DIGITAL_WALLET)
                .build();

        GatewayResult result = simulator.process("TXN-RR-TEST-WALLET", request);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getGatewayTransactionId()).startsWith("GW-TXN-");
        assertThat(result.getReceiptUrl()).isEqualTo("https://receipts.rentride.com/receipts/TXN-RR-TEST-WALLET.pdf");
    }
}


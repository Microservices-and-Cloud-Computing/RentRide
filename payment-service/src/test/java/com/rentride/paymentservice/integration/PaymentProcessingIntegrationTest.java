package com.rentride.paymentservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentride.paymentservice.dto.PaymentProcessRequest;
import com.rentride.paymentservice.entity.Currency;
import com.rentride.paymentservice.entity.PaymentMethod;
import com.rentride.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for Payment Processing, Gateway Simulation,
 * Idempotency Enforcement, and Transaction Verification.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentProcessingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("testProcessPayment_ApprovedCard_ReturnsSuccess: Send payload with customer 10 and card ending 4242 -> 200 OK, SUCCESS")
    void testProcessPayment_ApprovedCard_ReturnsSuccess() throws Exception {
        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(101L)
                .amount(new BigDecimal("25500.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111111111114242")
                .expiryMonth(12)
                .expiryYear(2028)
                .cvv("123")
                .build();

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.bookingId").value(101))
                .andExpect(jsonPath("$.customerId").value(10))
                .andExpect(jsonPath("$.amount").value(25500.00))
                .andExpect(jsonPath("$.currency").value("LKR"))
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.transactionReference", startsWith("TXN-RR-")))
                .andExpect(jsonPath("$.gatewayTransactionId", notNullValue()))
                .andExpect(jsonPath("$.gatewayTransactionId", startsWith("GW-TXN-")))
                .andExpect(jsonPath("$.receiptUrl", notNullValue()))
                .andExpect(jsonPath("$.receiptUrl", startsWith("https://receipts.rentride.com/receipts/")))
                .andExpect(jsonPath("$.failureReason").doesNotExist());
    }

    @Test
    @DisplayName("testProcessPayment_DeclinedCard_ReturnsFailed: Send payload with card ending 0000 -> 200 OK, FAILED, Insufficient funds")
    void testProcessPayment_DeclinedCard_ReturnsFailed() throws Exception {
        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(102L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111111111110000")
                .expiryMonth(10)
                .expiryYear(2027)
                .cvv("999")
                .build();

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.bookingId").value(102))
                .andExpect(jsonPath("$.customerId").value(10))
                .andExpect(jsonPath("$.amount").value(15000.00))
                .andExpect(jsonPath("$.failureReason").value("Insufficient funds"))
                .andExpect(jsonPath("$.gatewayTransactionId").doesNotExist())
                .andExpect(jsonPath("$.receiptUrl").doesNotExist());
    }

    @Test
    @DisplayName("testProcessPayment_DuplicatePayment_ThrowsConflict: Second payment for bookingId=55 throws HTTP 409 Conflict")
    void testProcessPayment_DuplicatePayment_ThrowsConflict() throws Exception {
        PaymentProcessRequest initialRequest = PaymentProcessRequest.builder()
                .bookingId(55L)
                .amount(new BigDecimal("8500.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111111111114242")
                .expiryMonth(12)
                .expiryYear(2028)
                .cvv("123")
                .build();

        // First attempt succeeds
        mockMvc.perform(post("/api/v1/payments/checkout")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Second attempt for the same bookingId triggers idempotency conflict guard
        PaymentProcessRequest duplicateRequest = PaymentProcessRequest.builder()
                .bookingId(55L)
                .amount(new BigDecimal("8500.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111111111114242")
                .expiryMonth(12)
                .expiryYear(2028)
                .cvv("123")
                .build();

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Booking has already been paid and confirmed"));
    }

    @Test
    @DisplayName("testVerifyTransaction_ValidReference_ReturnsVerifiedTrue: Query verify endpoint -> 200 OK, isVerified=true")
    void testVerifyTransaction_ValidReference_ReturnsVerifiedTrue() throws Exception {
        // 1. Create a successful payment
        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(200L)
                .amount(new BigDecimal("12000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111111111114242")
                .expiryMonth(11)
                .expiryYear(2028)
                .cvv("789")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/payments/checkout")
                        .header("X-User-Id", 25L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String txnRef = jsonNode.get("transactionReference").asText();

        // 2. Query public verification endpoint
        mockMvc.perform(get("/api/v1/payments/verify/{transactionReference}", txnRef))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference").value(txnRef))
                .andExpect(jsonPath("$.bookingId").value(200))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(12000.00))
                .andExpect(jsonPath("$.isVerified").value(true))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("testProcessPayment_GatewayTimeout_ReturnsFailedWithAcquiringTimeout")
    void testProcessPayment_GatewayTimeout_ReturnsFailedWithAcquiringTimeout() throws Exception {
        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(103L)
                .amount(new BigDecimal("18000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .cardNumber("4111111111119999")
                .expiryMonth(8)
                .expiryYear(2029)
                .cvv("555")
                .build();

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .header("X-User-Id", 15L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Acquiring network unresponsive"));
    }

    @Test
    @DisplayName("testVerifyTransaction_NotFound_Returns404")
    void testVerifyTransaction_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/payments/verify/{transactionReference}", "TXN-NON-EXISTENT-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString("TXN-NON-EXISTENT-999")));
    }
}


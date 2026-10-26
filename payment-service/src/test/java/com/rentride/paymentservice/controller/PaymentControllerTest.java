package com.rentride.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentride.paymentservice.dto.PaymentProcessRequest;
import com.rentride.paymentservice.dto.PaymentResponse;
import com.rentride.paymentservice.dto.TransactionVerifyResponse;
import com.rentride.paymentservice.entity.Currency;
import com.rentride.paymentservice.entity.PaymentMethod;
import com.rentride.paymentservice.entity.PaymentStatus;
import com.rentride.paymentservice.exception.GlobalExceptionHandler;
import com.rentride.paymentservice.exception.PaymentConflictException;
import com.rentride.paymentservice.exception.PaymentNotFoundException;
import com.rentride.paymentservice.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    @DisplayName("POST /api/v1/payments/checkout: Approved transaction returns HTTP 200 OK")
    void testCheckout_Success_Returns200() throws Exception {
        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(10L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111111111111234")
                .expiryMonth(12)
                .expiryYear(2027)
                .cvv("123")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .transactionReference("TXN-RR-20261026-OK001")
                .bookingId(10L)
                .customerId(5L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.SUCCESS)
                .gatewayTransactionId("GW-TXN-12345")
                .receiptUrl("https://receipts.rentride.com/receipts/TXN-RR-20261026-OK001.pdf")
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentService.processPayment(eq(5L), any(PaymentProcessRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .header("X-User-Id", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionReference").value("TXN-RR-20261026-OK001"))
                .andExpect(jsonPath("$.gatewayTransactionId").value("GW-TXN-12345"))
                .andExpect(jsonPath("$.receiptUrl").value("https://receipts.rentride.com/receipts/TXN-RR-20261026-OK001.pdf"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/checkout: Declined transaction returns HTTP 402 Payment Required")
    void testCheckout_Declined_Returns402() throws Exception {
        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(10L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111111111110000")
                .expiryMonth(12)
                .expiryYear(2027)
                .cvv("123")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(2L)
                .transactionReference("TXN-RR-20261026-FAIL01")
                .bookingId(10L)
                .customerId(5L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.FAILED)
                .failureReason("Insufficient funds")
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentService.processPayment(eq(5L), any(PaymentProcessRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .header("X-User-Id", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Insufficient funds"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/checkout: Duplicate payment attempt returns HTTP 409 Conflict")
    void testCheckout_DuplicatePayment_Returns409() throws Exception {
        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(10L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111111111111234")
                .expiryMonth(12)
                .expiryYear(2027)
                .cvv("123")
                .build();

        when(paymentService.processPayment(eq(5L), any(PaymentProcessRequest.class)))
                .thenThrow(new PaymentConflictException("Booking has already been paid and confirmed"));

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .header("X-User-Id", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Booking has already been paid and confirmed"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/checkout: Validation failure (e.g. invalid CVV) returns HTTP 400 Bad Request")
    void testCheckout_InvalidCardFormat_Returns400() throws Exception {
        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(10L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("1234") // Invalid length
                .expiryMonth(12)
                .expiryYear(2027)
                .cvv("12") // Invalid length
                .build();

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .header("X-User-Id", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/v1/payments/verify/{transactionReference}: Public verification returns HTTP 200 OK")
    void testVerifyTransaction_Success_Returns200() throws Exception {
        TransactionVerifyResponse verifyResponse = TransactionVerifyResponse.builder()
                .transactionReference("TXN-RR-VERIFY-001")
                .bookingId(55L)
                .status(PaymentStatus.SUCCESS)
                .amount(new BigDecimal("20000.00"))
                .isVerified(true)
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentService.verifyTransaction("TXN-RR-VERIFY-001")).thenReturn(verifyResponse);

        mockMvc.perform(get("/api/v1/payments/verify/TXN-RR-VERIFY-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionReference").value("TXN-RR-VERIFY-001"))
                .andExpect(jsonPath("$.isVerified").value(true))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.bookingId").value(55));
    }

    @Test
    @DisplayName("GET /api/v1/payments/booking/{bookingId}: Returns payment details with HTTP 200 OK")
    void testGetPaymentByBookingId_Success_Returns200() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .id(1L)
                .transactionReference("TXN-RR-BK-1234")
                .bookingId(77L)
                .customerId(9L)
                .amount(new BigDecimal("8000.00"))
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getPaymentByBookingId(77L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/booking/77"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(77))
                .andExpect(jsonPath("$.transactionReference").value("TXN-RR-BK-1234"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/booking/{bookingId}: Not found returns HTTP 404")
    void testGetPaymentByBookingId_NotFound_Returns404() throws Exception {
        when(paymentService.getPaymentByBookingId(999L))
                .thenThrow(new PaymentNotFoundException("No payment records found for booking ID: 999"));

        mockMvc.perform(get("/api/v1/payments/booking/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}


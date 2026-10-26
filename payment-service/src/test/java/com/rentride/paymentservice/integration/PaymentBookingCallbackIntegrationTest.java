package com.rentride.paymentservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.rentride.paymentservice.dto.PaymentProcessRequest;
import com.rentride.paymentservice.entity.Currency;
import com.rentride.paymentservice.entity.Payment;
import com.rentride.paymentservice.entity.PaymentMethod;
import com.rentride.paymentservice.entity.PaymentStatus;
import com.rentride.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests verifying OpenFeign Payment-to-Booking RPC callback
 * and distributed state synchronization using declarative WireMock stubs (Commit 28).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "booking-service.url=http://localhost:${wiremock.server.port}"
})
class PaymentBookingCallbackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        WireMock.reset();
    }

    @Test
    @DisplayName("Should successfully process payment and trigger booking CONFIRMED callback via OpenFeign")
    void testPaymentSuccess_TriggersBookingConfirmedCallback() throws Exception {
        // Arrange WireMock stubs for Booking-Service
        stubFor(get(urlEqualTo("/api/v1/bookings/101"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                {
                                  "id": 101,
                                  "bookingReference": "RR-BK-20261026-TEST01",
                                  "status": "PENDING_PAYMENT",
                                  "totalCost": 15000.00
                                }
                                """)));

        stubFor(patch(urlEqualTo("/api/v1/bookings/101/payment-status"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                {
                                  "id": 101,
                                  "bookingReference": "RR-BK-20261026-TEST01",
                                  "status": "CONFIRMED",
                                  "totalCost": 15000.00
                                }
                                """)));

        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(101L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4242424242424242")
                .expiryMonth(12)
                .expiryYear(2028)
                .cvv("123")
                .build();

        // Act: Perform checkout
        mockMvc.perform(post("/api/v1/payments/checkout")
                        .header("X-User-Id", 10L)
                        .header("X-User-Role", "ROLE_CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.bookingId").value(101))
                .andExpect(jsonPath("$.amount").value(15000.00))
                .andExpect(jsonPath("$.transactionReference").isNotEmpty())
                .andExpect(jsonPath("$.gatewayTransactionId").isNotEmpty())
                .andExpect(jsonPath("$.receiptUrl").isNotEmpty());

        // Assert: Database persistence
        List<Payment> payments = paymentRepository.findByBookingId(101L);
        assertThat(payments).hasSize(1);
        Payment payment = payments.get(0);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        String txnRef = payment.getTransactionReference();

        // Assert: Verify WireMock captured GET booking query
        WireMock.verify(getRequestedFor(urlEqualTo("/api/v1/bookings/101")));

        // Assert: Verify WireMock captured exact PATCH callback request payload
        WireMock.verify(patchRequestedFor(urlEqualTo("/api/v1/bookings/101/payment-status"))
                .withHeader("Content-Type", WireMock.containing("application/json"))
                .withRequestBody(WireMock.matchingJsonPath("$.paymentStatus", WireMock.equalTo("SUCCESS")))
                .withRequestBody(WireMock.matchingJsonPath("$.paidAmount", WireMock.matching("15000(\\.0+)?")))
                .withRequestBody(WireMock.matchingJsonPath("$.transactionReference", WireMock.equalTo(txnRef))));
    }

    @Test
    @DisplayName("Should reject payment with 400 Bad Request before processing when amount mismatches booking totalCost")
    void testPayment_AmountMismatch_RejectsBeforeProcessing() throws Exception {
        // Arrange: WireMock returns totalCost of 20000.00
        stubFor(get(urlEqualTo("/api/v1/bookings/101"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                {
                                  "id": 101,
                                  "bookingReference": "RR-BK-20261026-TEST01",
                                  "status": "PENDING_PAYMENT",
                                  "totalCost": 20000.00
                                }
                                """)));

        // Request offers only 15000.00
        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(101L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4242424242424242")
                .expiryMonth(12)
                .expiryYear(2028)
                .cvv("123")
                .build();

        // Act & Assert: Rejected before payment processing
        mockMvc.perform(post("/api/v1/payments/checkout")
                        .header("X-User-Id", 10L)
                        .header("X-User-Role", "ROLE_CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Payment amount 15000.00 does not match booking total cost 20000.00")));

        // Assert: Gateway was not called, no payment was persisted
        assertThat(paymentRepository.findByBookingId(101L)).isEmpty();
        WireMock.verify(0, patchRequestedFor(urlEqualTo("/api/v1/bookings/101/payment-status")));
    }

    @Test
    @DisplayName("Should reject payment with 409 Conflict when booking is already CONFIRMED")
    void testPayment_BookingAlreadyConfirmed_RejectsPayment() throws Exception {
        // Arrange: WireMock returns status CONFIRMED
        stubFor(get(urlEqualTo("/api/v1/bookings/101"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                {
                                  "id": 101,
                                  "bookingReference": "RR-BK-20261026-TEST01",
                                  "status": "CONFIRMED",
                                  "totalCost": 15000.00
                                }
                                """)));

        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(101L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4242424242424242")
                .expiryMonth(12)
                .expiryYear(2028)
                .cvv("123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/checkout")
                        .header("X-User-Id", 10L)
                        .header("X-User-Role", "ROLE_CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("cannot be paid: current status is CONFIRMED (must be PENDING_PAYMENT)")));

        assertThat(paymentRepository.findByBookingId(101L)).isEmpty();
        WireMock.verify(0, patchRequestedFor(urlEqualTo("/api/v1/bookings/101/payment-status")));
    }

    @Test
    @DisplayName("Should notify booking-service of FAILED status when payment gateway declines transaction")
    void testPaymentFailed_TriggersBookingFailedCallback() throws Exception {
        // Arrange WireMock stubs
        stubFor(get(urlEqualTo("/api/v1/bookings/102"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                {
                                  "id": 102,
                                  "bookingReference": "RR-BK-20261026-TEST02",
                                  "status": "PENDING_PAYMENT",
                                  "totalCost": 15000.00
                                }
                                """)));

        stubFor(patch(urlEqualTo("/api/v1/bookings/102/payment-status"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                {
                                  "id": 102,
                                  "bookingReference": "RR-BK-20261026-TEST02",
                                  "status": "PENDING_PAYMENT",
                                  "totalCost": 15000.00
                                }
                                """)));

        // Card ending with '0000' simulates card decline / insufficient funds
        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(102L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4242424242420000")
                .expiryMonth(12)
                .expiryYear(2028)
                .cvv("123")
                .build();

        // Act: Perform checkout
        mockMvc.perform(post("/api/v1/payments/checkout")
                        .header("X-User-Id", 10L)
                        .header("X-User-Role", "ROLE_CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Insufficient funds"));

        // Assert: WireMock callback invoked with FAILED status
        WireMock.verify(patchRequestedFor(urlEqualTo("/api/v1/bookings/102/payment-status"))
                .withRequestBody(WireMock.matchingJsonPath("$.paymentStatus", WireMock.equalTo("FAILED"))));
    }

    @Test
    @DisplayName("Should return 404 Not Found when booking does not exist in booking-service")
    void testPayment_BookingNotFound_Returns404() throws Exception {
        stubFor(get(urlEqualTo("/api/v1/bookings/999"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(404)
                        .withBody("""
                                {
                                  "message": "Booking not found with id: 999"
                                }
                                """)));

        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(999L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4242424242424242")
                .expiryMonth(12)
                .expiryYear(2028)
                .cvv("123")
                .build();

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .header("X-User-Id", 10L)
                        .header("X-User-Role", "ROLE_CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Booking not found")));

        assertThat(paymentRepository.findByBookingId(999L)).isEmpty();
    }

    @Test
    @DisplayName("Should mark payment as REQUIRES_RECONCILIATION when booking callback fails after successful charge")
    void testPaymentSuccess_BookingServiceUnavailable_TransitionsToRequiresReconciliation() throws Exception {
        stubFor(get(urlEqualTo("/api/v1/bookings/103"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                {
                                  "id": 103,
                                  "bookingReference": "RR-BK-20261026-TEST03",
                                  "status": "PENDING_PAYMENT",
                                  "totalCost": 15000.00
                                }
                                """)));

        // Downstream booking status update fails with 503 Service Unavailable
        stubFor(patch(urlEqualTo("/api/v1/bookings/103/payment-status"))
                .willReturn(aResponse()
                        .withStatus(503)));

        PaymentProcessRequest request = PaymentProcessRequest.builder()
                .bookingId(103L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4242424242424242")
                .expiryMonth(12)
                .expiryYear(2028)
                .cvv("123")
                .build();

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .header("X-User-Id", 10L)
                        .header("X-User-Role", "ROLE_CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUIRES_RECONCILIATION"));

        List<Payment> payments = paymentRepository.findByBookingId(103L);
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.REQUIRES_RECONCILIATION);
    }
}

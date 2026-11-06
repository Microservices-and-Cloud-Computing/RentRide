package com.rentride.paymentservice.service;

import com.rentride.paymentservice.client.BookingServiceClient;
import com.rentride.paymentservice.dto.PaymentProcessRequest;
import com.rentride.paymentservice.dto.PaymentResponse;
import com.rentride.paymentservice.dto.TransactionVerifyResponse;
import com.rentride.paymentservice.dto.external.BookingPaymentCallbackRequest;
import com.rentride.paymentservice.dto.external.BookingSummaryResponse;
import com.rentride.paymentservice.entity.Currency;
import com.rentride.paymentservice.entity.Payment;
import com.rentride.paymentservice.entity.PaymentMethod;
import com.rentride.paymentservice.entity.PaymentStatus;
import com.rentride.paymentservice.exception.BookingNotFoundException;
import com.rentride.paymentservice.exception.InvalidPaymentDetailsException;
import com.rentride.paymentservice.exception.PaymentConflictException;
import com.rentride.paymentservice.exception.PaymentNotFoundException;
import com.rentride.paymentservice.repository.PaymentRepository;
import com.rentride.paymentservice.service.gateway.GatewayResult;
import com.rentride.paymentservice.service.gateway.PaymentGatewaySimulator;
import com.rentride.paymentservice.util.TransactionReferenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGatewaySimulator paymentGatewaySimulator;

    @Mock
    private TransactionReferenceGenerator transactionReferenceGenerator;

    @Mock
    private BookingServiceClient bookingServiceClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentProcessRequest validCardRequest;
    private BookingSummaryResponse pendingBooking;

    @BeforeEach
    void setUp() {
        validCardRequest = PaymentProcessRequest.builder()
                .bookingId(100L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .cardNumber("4111111111111234")
                .expiryMonth(12)
                .expiryYear(2028)
                .cvv("123")
                .build();

        pendingBooking = BookingSummaryResponse.builder()
                .id(100L)
                .bookingReference("RR-BK-20261026-TEST")
                .status("PENDING_PAYMENT")
                .totalCost(new BigDecimal("15000.00"))
                .customerId(5L)
                .build();
    }

    @Test
    @DisplayName("processPayment: Successful gateway authorization updates status to SUCCESS, notifies booking-service and returns response")
    void testProcessPayment_Success() {
        when(paymentRepository.existsByBookingIdAndStatus(100L, PaymentStatus.SUCCESS)).thenReturn(false);
        when(bookingServiceClient.getBookingById(100L)).thenReturn(pendingBooking);
        when(transactionReferenceGenerator.generate()).thenReturn("TXN-RR-20261026-ABC123");

        Payment pendingPayment = Payment.builder()
                .id(1L)
                .transactionReference("TXN-RR-20261026-ABC123")
                .bookingId(100L)
                .customerId(5L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(pendingPayment);

        GatewayResult gatewayResult = GatewayResult.success(
                "GW-TXN-UUID-999",
                "https://receipts.rentride.com/receipts/TXN-RR-20261026-ABC123.pdf"
        );
        when(paymentGatewaySimulator.process(eq("TXN-RR-20261026-ABC123"), any())).thenReturn(gatewayResult);

        PaymentResponse response = paymentService.processPayment(5L, validCardRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getGatewayTransactionId()).isEqualTo("GW-TXN-UUID-999");
        assertThat(response.getReceiptUrl()).isEqualTo("https://receipts.rentride.com/receipts/TXN-RR-20261026-ABC123.pdf");
        assertThat(response.getTransactionReference()).isEqualTo("TXN-RR-20261026-ABC123");

        // Verify OpenFeign callback
        verify(bookingServiceClient).updatePaymentStatus(eq(100L), any(BookingPaymentCallbackRequest.class));
    }

    @Test
    @DisplayName("processPayment: Gateway decline sets status to FAILED and notifies booking-service")
    void testProcessPayment_GatewayDeclined_SetsFailedStatus() {
        when(paymentRepository.existsByBookingIdAndStatus(100L, PaymentStatus.SUCCESS)).thenReturn(false);
        when(bookingServiceClient.getBookingById(100L)).thenReturn(pendingBooking);
        when(transactionReferenceGenerator.generate()).thenReturn("TXN-RR-20261026-FAIL01");

        Payment pendingPayment = Payment.builder()
                .id(2L)
                .transactionReference("TXN-RR-20261026-FAIL01")
                .bookingId(100L)
                .customerId(5L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(pendingPayment);

        GatewayResult gatewayResult = GatewayResult.failed("Insufficient funds");
        when(paymentGatewaySimulator.process(eq("TXN-RR-20261026-FAIL01"), any())).thenReturn(gatewayResult);

        PaymentResponse response = paymentService.processPayment(5L, validCardRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.getFailureReason()).isEqualTo("Insufficient funds");
        assertThat(response.getGatewayTransactionId()).isNull();

        verify(bookingServiceClient).updatePaymentStatus(eq(100L), any(BookingPaymentCallbackRequest.class));
    }

    @Test
    @DisplayName("processPayment: Throws PaymentConflictException when booking has already been paid")
    void testProcessPayment_DuplicateBooking_ThrowsConflict() {
        when(paymentRepository.existsByBookingIdAndStatus(100L, PaymentStatus.SUCCESS)).thenReturn(true);

        assertThatThrownBy(() -> paymentService.processPayment(5L, validCardRequest))
                .isInstanceOf(PaymentConflictException.class)
                .hasMessageContaining("Booking has already been paid and confirmed");

        verify(bookingServiceClient, never()).getBookingById(any());
        verify(paymentGatewaySimulator, never()).process(any(), any());
    }

    @Test
    @DisplayName("processPayment: Pre-check booking not found throws BookingNotFoundException")
    void testProcessPayment_BookingNotFound_ThrowsException() {
        when(paymentRepository.existsByBookingIdAndStatus(100L, PaymentStatus.SUCCESS)).thenReturn(false);
        when(bookingServiceClient.getBookingById(100L)).thenReturn(null);

        assertThatThrownBy(() -> paymentService.processPayment(5L, validCardRequest))
                .isInstanceOf(BookingNotFoundException.class);

        verify(paymentGatewaySimulator, never()).process(any(), any());
    }

    @Test
    @DisplayName("processPayment: Pre-check booking in non-PENDING_PAYMENT status throws IllegalStateException")
    void testProcessPayment_InvalidBookingStatus_ThrowsException() {
        when(paymentRepository.existsByBookingIdAndStatus(100L, PaymentStatus.SUCCESS)).thenReturn(false);
        BookingSummaryResponse confirmedBooking = BookingSummaryResponse.builder()
                .id(100L)
                .bookingReference("RR-BK-100")
                .status("CONFIRMED")
                .totalCost(new BigDecimal("15000.00"))
                .build();
        when(bookingServiceClient.getBookingById(100L)).thenReturn(confirmedBooking);

        assertThatThrownBy(() -> paymentService.processPayment(5L, validCardRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be PENDING_PAYMENT");

        verify(paymentGatewaySimulator, never()).process(any(), any());
    }

    @Test
    @DisplayName("processPayment: Pre-check amount mismatch throws InvalidPaymentDetailsException")
    void testProcessPayment_AmountMismatch_ThrowsException() {
        when(paymentRepository.existsByBookingIdAndStatus(100L, PaymentStatus.SUCCESS)).thenReturn(false);
        BookingSummaryResponse differentCostBooking = BookingSummaryResponse.builder()
                .id(100L)
                .bookingReference("RR-BK-100")
                .status("PENDING_PAYMENT")
                .totalCost(new BigDecimal("20000.00")) // Expected 20,000 but request is 15,000
                .build();
        when(bookingServiceClient.getBookingById(100L)).thenReturn(differentCostBooking);

        assertThatThrownBy(() -> paymentService.processPayment(5L, validCardRequest))
                .isInstanceOf(InvalidPaymentDetailsException.class)
                .hasMessageContaining("does not match booking total cost");

        verify(paymentGatewaySimulator, never()).process(any(), any());
    }

    @Test
    @DisplayName("processPayment: Feign callback failure on success marks payment as REQUIRES_RECONCILIATION")
    void testProcessPayment_CallbackFails_MarksRequiresReconciliation() {
        when(paymentRepository.existsByBookingIdAndStatus(100L, PaymentStatus.SUCCESS)).thenReturn(false);
        when(bookingServiceClient.getBookingById(100L)).thenReturn(pendingBooking);
        when(transactionReferenceGenerator.generate()).thenReturn("TXN-RR-20261026-RECON");

        Payment pendingPayment = Payment.builder()
                .id(3L)
                .transactionReference("TXN-RR-20261026-RECON")
                .bookingId(100L)
                .customerId(5L)
                .amount(new BigDecimal("15000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(pendingPayment);

        GatewayResult gatewayResult = GatewayResult.success(
                "GW-TXN-RECON-1",
                "https://receipts.rentride.com/receipts/TXN-RR-20261026-RECON.pdf"
        );
        when(paymentGatewaySimulator.process(eq("TXN-RR-20261026-RECON"), any())).thenReturn(gatewayResult);

        // Simulate Feign failure during callback
        when(bookingServiceClient.updatePaymentStatus(eq(100L), any(BookingPaymentCallbackRequest.class)))
                .thenThrow(new RuntimeException("Connection timed out to booking-service"));

        PaymentResponse response = paymentService.processPayment(5L, validCardRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REQUIRES_RECONCILIATION);
        assertThat(response.getFailureReason()).contains("Booking status update callback failed");
    }

    @Test
    @DisplayName("verifyTransaction: Returns isVerified=true when transaction status is SUCCESS")
    void testVerifyTransaction_Success() {
        Payment payment = Payment.builder()
                .id(1L)
                .transactionReference("TXN-RR-VERIFY-001")
                .bookingId(100L)
                .customerId(5L)
                .amount(new BigDecimal("15000.00"))
                .status(PaymentStatus.SUCCESS)
                .build();
        payment.setCreatedAt(LocalDateTime.now());

        when(paymentRepository.findByTransactionReference("TXN-RR-VERIFY-001")).thenReturn(Optional.of(payment));

        TransactionVerifyResponse response = paymentService.verifyTransaction("TXN-RR-VERIFY-001");

        assertThat(response).isNotNull();
        assertThat(response.getTransactionReference()).isEqualTo("TXN-RR-VERIFY-001");
        assertThat(response.getIsVerified()).isTrue();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("15000.00"));
    }

    @Test
    @DisplayName("verifyTransaction: Throws PaymentNotFoundException for non-existent reference")
    void testVerifyTransaction_NotFound_ThrowsException() {
        when(paymentRepository.findByTransactionReference("TXN-INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.verifyTransaction("TXN-INVALID"))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment transaction not found");
    }

    @Test
    @DisplayName("getPaymentByBookingId: Returns latest payment attempt for booking")
    void testGetPaymentByBookingId_Success() {
        Payment first = Payment.builder()
                .id(10L)
                .transactionReference("TXN-RR-01")
                .bookingId(200L)
                .customerId(5L)
                .amount(new BigDecimal("8000.00"))
                .status(PaymentStatus.FAILED)
                .failureReason("Declined")
                .build();

        Payment second = Payment.builder()
                .id(11L)
                .transactionReference("TXN-RR-02")
                .bookingId(200L)
                .customerId(5L)
                .amount(new BigDecimal("8000.00"))
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentRepository.findByBookingId(200L)).thenReturn(List.of(first, second));

        PaymentResponse response = paymentService.getPaymentByBookingId(200L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getTransactionReference()).isEqualTo("TXN-RR-02");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("getPaymentByBookingId: Throws PaymentNotFoundException when booking has no payments")
    void testGetPaymentByBookingId_NotFound_ThrowsException() {
        when(paymentRepository.findByBookingId(999L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> paymentService.getPaymentByBookingId(999L))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("No payment records found for booking ID: 999");
    }
}

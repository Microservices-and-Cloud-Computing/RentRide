package com.rentride.notificationservice.messaging;

import com.rentride.notificationservice.dto.EmailNotificationRequest;
import com.rentride.notificationservice.event.BookingConfirmedEvent;
import com.rentride.notificationservice.event.PaymentProcessedEvent;
import com.rentride.notificationservice.event.RentalLifecycleEvent;
import com.rentride.notificationservice.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test suite for {@link NotificationEventListener}.
 *
 * <p>Uses {@code @SpringBootTest} with the {@code "test"} profile (Eureka disabled)
 * and mocks both {@link EmailService} and {@link org.springframework.mail.javamail.JavaMailSender}
 * so no live RabbitMQ or SMTP server is required.
 *
 * <p>The listener methods are exercised directly (bypassing AMQP transport) to verify:
 * <ul>
 *   <li>Correct template selection and model population.</li>
 *   <li>Idempotent async email dispatch delegation.</li>
 *   <li>Payment success vs failure routing.</li>
 *   <li>Exception re-throw triggering AMQP retry/dead-lettering.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "management.health.mail.enabled=false",
        "management.health.rabbit.enabled=false",
        // Disable RabbitMQ auto-connection — listener methods called directly in tests
        "spring.rabbitmq.host=localhost",
        "spring.rabbitmq.port=5672"
})
class NotificationEventListenerTest {

    @Autowired
    private NotificationEventListener listener;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    // ── Test 1: Booking Confirmed → booking-confirmed template ───────────────

    @Test
    @DisplayName("handleBookingConfirmed — dispatches booking-confirmed email with correct model")
    void testHandleBookingConfirmed_TriggersBookingEmail() {
        // Given
        BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                .bookingReference("RR-BK-20261018-7711")
                .customerEmail("kasun@example.com")
                .customerName("Kasun Perera")
                .providerEmail("fleet@rentride.com")
                .vehicleName("Toyota Aqua 2021")
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 4))
                .totalAmount(new BigDecimal("25500.00"))
                .pickupLocation("Colombo Central")
                .occurredOn(LocalDateTime.of(2026, 10, 18, 21, 30))
                .build();

        // When
        listener.handleBookingConfirmed(event);

        // Then
        ArgumentCaptor<EmailNotificationRequest> captor =
                ArgumentCaptor.forClass(EmailNotificationRequest.class);
        verify(emailService, times(1)).sendAsyncEmail(captor.capture());

        EmailNotificationRequest captured = captor.getValue();
        assertThat(captured.getRecipientEmail()).isEqualTo("kasun@example.com");
        assertThat(captured.getTemplateName()).isEqualTo("booking-confirmed");
        assertThat(captured.getSubject()).contains("RR-BK-20261018-7711");
        assertThat(captured.getTemplateModel()).containsEntry("customerName", "Kasun Perera");
        assertThat(captured.getTemplateModel()).containsEntry("bookingReference", "RR-BK-20261018-7711");
        assertThat(captured.getTemplateModel()).containsKey("totalAmount");
    }

    // ── Test 2: Payment SUCCESS → payment-receipt template ───────────────────

    @Test
    @DisplayName("handlePaymentProcessed(SUCCESS) — dispatches payment-receipt email")
    void testHandlePaymentProcessed_Success_TriggersReceiptEmail() {
        // Given
        PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                .transactionReference("TXN-20261101-001")
                .bookingReference("RR-BK-20261018-7711")
                .customerEmail("kasun@example.com")
                .customerName("Kasun Perera")
                .amount(new BigDecimal("25500.00"))
                .currency("LKR")
                .paymentMethod("CREDIT_CARD")
                .status("SUCCESS")
                .occurredOn(LocalDateTime.of(2026, 11, 1, 10, 0))
                .build();

        // When
        listener.handlePaymentProcessed(event);

        // Then
        ArgumentCaptor<EmailNotificationRequest> captor =
                ArgumentCaptor.forClass(EmailNotificationRequest.class);
        verify(emailService, times(1)).sendAsyncEmail(captor.capture());

        EmailNotificationRequest captured = captor.getValue();
        assertThat(captured.getTemplateName()).isEqualTo("payment-receipt");
        assertThat(captured.getSubject()).contains("TXN-20261101-001");
        assertThat(captured.getTemplateModel()).containsEntry("transactionReference", "TXN-20261101-001");
        assertThat(captured.getTemplateModel()).containsEntry("currency", "LKR");
        assertThat(captured.getTemplateModel()).containsKey("amount");
    }

    // ── Test 3: Payment FAILED → failure-alert email ─────────────────────────

    @Test
    @DisplayName("handlePaymentProcessed(FAILED) — dispatches payment failure alert email")
    void testHandlePaymentProcessed_Failed_TriggersFailureAlert() {
        // Given
        PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                .transactionReference("TXN-20261101-002")
                .bookingReference("RR-BK-20261018-7711")
                .customerEmail("kasun@example.com")
                .customerName("Kasun Perera")
                .amount(new BigDecimal("25500.00"))
                .currency("LKR")
                .paymentMethod("DEBIT_CARD")
                .status("FAILED")
                .failureReason("Insufficient funds")
                .occurredOn(LocalDateTime.of(2026, 11, 1, 10, 5))
                .build();

        // When
        listener.handlePaymentProcessed(event);

        // Then
        ArgumentCaptor<EmailNotificationRequest> captor =
                ArgumentCaptor.forClass(EmailNotificationRequest.class);
        verify(emailService, times(1)).sendAsyncEmail(captor.capture());

        EmailNotificationRequest captured = captor.getValue();
        assertThat(captured.getTemplateName()).isEqualTo("payment-receipt");
        assertThat(captured.getSubject()).contains("⚠").contains("Failed");
        assertThat(captured.getTemplateModel()).containsEntry("paymentStatus", "FAILED");
        assertThat(captured.getTemplateModel()).containsEntry("failureReason", "Insufficient funds");
    }

    // ── Test 4: Rental PICKUP → rental-completed template ────────────────────

    @Test
    @DisplayName("handleRentalLifecycle(PICKUP_COMPLETED) — dispatches pickup confirmation email")
    void testHandleRentalLifecycle_PickupCompleted_TriggersPickupEmail() {
        // Given
        RentalLifecycleEvent event = RentalLifecycleEvent.builder()
                .bookingReference("RR-BK-20261018-7711")
                .customerEmail("kasun@example.com")
                .customerName("Kasun Perera")
                .vehicleName("Toyota Aqua 2021")
                .eventType("PICKUP_COMPLETED")
                .odometerReading(45000.0)
                .vehicleCondition("Excellent")
                .additionalCharges(BigDecimal.ZERO)
                .occurredOn(LocalDateTime.of(2026, 11, 1, 9, 0))
                .build();

        // When
        listener.handleRentalLifecycle(event);

        // Then
        ArgumentCaptor<EmailNotificationRequest> captor =
                ArgumentCaptor.forClass(EmailNotificationRequest.class);
        verify(emailService, times(1)).sendAsyncEmail(captor.capture());

        EmailNotificationRequest captured = captor.getValue();
        assertThat(captured.getTemplateName()).isEqualTo("rental-completed");
        assertThat(captured.getSubject()).contains("Pickup");
        assertThat(captured.getTemplateModel()).containsEntry("eventType", "PICKUP_COMPLETED");
    }

    // ── Test 5: Rental RETURN → rental-completed template ────────────────────

    @Test
    @DisplayName("handleRentalLifecycle(RETURN_COMPLETED) — dispatches return receipt email")
    void testHandleRentalLifecycle_ReturnCompleted_TriggersReturnReceipt() {
        // Given
        RentalLifecycleEvent event = RentalLifecycleEvent.builder()
                .bookingReference("RR-BK-20261018-7711")
                .customerEmail("kasun@example.com")
                .customerName("Kasun Perera")
                .vehicleName("Toyota Aqua 2021")
                .eventType("RETURN_COMPLETED")
                .odometerReading(45320.0)
                .vehicleCondition("Good")
                .additionalCharges(new BigDecimal("1500.00"))
                .occurredOn(LocalDateTime.of(2026, 11, 4, 14, 30))
                .build();

        // When
        listener.handleRentalLifecycle(event);

        // Then
        ArgumentCaptor<EmailNotificationRequest> captor =
                ArgumentCaptor.forClass(EmailNotificationRequest.class);
        verify(emailService, times(1)).sendAsyncEmail(captor.capture());

        EmailNotificationRequest captured = captor.getValue();
        assertThat(captured.getTemplateName()).isEqualTo("rental-completed");
        assertThat(captured.getSubject()).contains("Return Receipt");
        assertThat(captured.getTemplateModel()).containsEntry("returnCondition", "Good");
        assertThat(captured.getTemplateModel()).containsEntry("eventType", "RETURN_COMPLETED");
    }

    // ── Test 6: Exception re-thrown for AMQP retry / dead-lettering ──────────

    @Test
    @DisplayName("handleBookingConfirmed — exception is re-thrown to trigger AMQP retry/DLQ routing")
    void testHandleBookingConfirmed_ExceptionRethrown_TriggersRetryAndDeadLetter() {
        // Given — emailService.sendAsyncEmail throws a runtime exception
        doThrow(new RuntimeException("Simulated downstream failure"))
                .when(emailService).sendAsyncEmail(any(EmailNotificationRequest.class));

        BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                .bookingReference("RR-BK-POISON-001")
                .customerEmail("bad@example.com")
                .customerName("Test User")
                .vehicleName("Test Vehicle")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .totalAmount(BigDecimal.TEN)
                .pickupLocation("Test Location")
                .occurredOn(LocalDateTime.now())
                .build();

        // When / Then — the listener must re-throw so AMQP can retry and dead-letter
        assertThrows(RuntimeException.class,
                () -> listener.handleBookingConfirmed(event),
                "Listener must re-throw exceptions to trigger AMQP retry/DLQ routing");
    }

    @BeforeEach
    void resetMocks() {
        reset(emailService);
    }
}


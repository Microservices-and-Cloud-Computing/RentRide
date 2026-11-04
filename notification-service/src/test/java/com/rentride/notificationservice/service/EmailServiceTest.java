package com.rentride.notificationservice.service;

import com.rentride.notificationservice.dto.EmailNotificationRequest;
import com.rentride.notificationservice.dto.EmailNotificationResponse;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration / slice test suite for {@link EmailService} and {@link EmailServiceImpl}.
 *
 * <p>Uses {@code @SpringBootTest(excludeAutoConfiguration = {
        MailHealthContributorAutoConfiguration.class
})} to load the full Spring context (including
 * {@link org.thymeleaf.spring6.SpringTemplateEngine} and {@link org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor})
 * while mocking the {@link JavaMailSender} to avoid requiring a live SMTP server.
 *
 * <p>Test coverage:
 * <ul>
 *   <li>Successful synchronous email dispatch with Thymeleaf template rendering</li>
 *   <li>Graceful FAILED response on invalid template name (no crash)</li>
 *   <li>Asynchronous dispatch returns immediately; background thread handles delivery</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // Disable Actuator mail health check — JavaMailSender is mocked and
        // MailHealthContributorAutoConfiguration requires a real bean map (non-empty).
        "management.health.mail.enabled=false"
})
class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @MockitoBean
    private JavaMailSender mailSender;

    private MimeMessage fakeMimeMessage;

    @BeforeEach
    void setUp() throws Exception {
        // Use a real Session-less MimeMessage backed by an empty session
        fakeMimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(fakeMimeMessage);
        // Default: send() does nothing (void) — simulates successful SMTP delivery
        doNothing().when(mailSender).send(any(MimeMessage.class));
    }

    // ── Test 1: Booking Confirmed template renders and delivers ───────────────

    @Test
    @DisplayName("sendHtmlEmail — booking-confirmed template renders variables and returns SENT")
    void testSendHtmlEmail_BookingConfirmed_RendersVariablesAndDelivers() {
        // Given
        EmailNotificationRequest request = EmailNotificationRequest.builder()
                .recipientEmail("kasun@example.com")
                .recipientName("Kasun Perera")
                .subject("Your RentRide Booking is Confirmed!")
                .templateName("booking-confirmed")
                .templateModel(Map.of(
                        "customerName",      "Kasun Perera",
                        "bookingReference",  "RR-BK-9912",
                        "vehicleName",       "Toyota Prius 2021",
                        "startDate",         "2026-11-01",
                        "endDate",           "2026-11-04",
                        "pickupLocation",    "Colombo Central",
                        "totalAmount",       "18,000.00"
                ))
                .build();

        // When
        EmailNotificationResponse response = emailService.sendHtmlEmail(request);

        // Then — response assertions
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("SENT");
        assertThat(response.getRecipientEmail()).isEqualTo("kasun@example.com");
        assertThat(response.getNotificationId()).isNotBlank();
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getErrorMessage()).isNull();

        // Verify mailSender was invoked exactly once
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // ── Test 2: Payment receipt template ─────────────────────────────────────

    @Test
    @DisplayName("sendHtmlEmail — payment-receipt template renders financial details and returns SENT")
    void testSendHtmlEmail_PaymentReceipt_ReturnsStatusSent() {
        // Given
        EmailNotificationRequest request = EmailNotificationRequest.builder()
                .recipientEmail("kasun@example.com")
                .recipientName("Kasun Perera")
                .subject("Your RentRide Payment Receipt")
                .templateName("payment-receipt")
                .templateModel(Map.of(
                        "customerName",          "Kasun Perera",
                        "transactionReference",  "TXN-20261101-001",
                        "bookingReference",      "RR-BK-9912",
                        "paymentMethod",         "CREDIT_CARD",
                        "currency",              "LKR",
                        "amount",                "18,000.00",
                        "paymentTimestamp",      "2026-11-01 10:00"
                ))
                .build();

        // When
        EmailNotificationResponse response = emailService.sendHtmlEmail(request);

        // Then
        assertThat(response.getStatus()).isEqualTo("SENT");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // ── Test 3: Rental completed template ────────────────────────────────────

    @Test
    @DisplayName("sendHtmlEmail — rental-completed template renders return details and returns SENT")
    void testSendHtmlEmail_RentalCompleted_ReturnsStatusSent() {
        // Given
        EmailNotificationRequest request = EmailNotificationRequest.builder()
                .recipientEmail("kasun@example.com")
                .recipientName("Kasun Perera")
                .subject("Your RentRide Rental is Complete")
                .templateName("rental-completed")
                .templateModel(Map.of(
                        "customerName",      "Kasun Perera",
                        "bookingReference",  "RR-BK-9912",
                        "vehicleName",       "Toyota Prius 2021",
                        "returnDateTime",    "2026-11-04 14:30",
                        "finalOdometer",     "48,320",
                        "returnCondition",   "Good",
                        "additionalCharges", "0.00",
                        "totalAmount",       "18,000.00"
                ))
                .build();

        // When
        EmailNotificationResponse response = emailService.sendHtmlEmail(request);

        // Then
        assertThat(response.getStatus()).isEqualTo("SENT");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // ── Test 4: Invalid template → graceful FAILED response ──────────────────

    @Test
    @DisplayName("sendHtmlEmail — invalid template name returns FAILED without uncaught exception")
    void testSendHtmlEmail_InvalidTemplate_HandlesGracefully() {
        // Given — nonexistent template; Thymeleaf will throw TemplateInputException
        EmailNotificationRequest request = EmailNotificationRequest.builder()
                .recipientEmail("kasun@example.com")
                .recipientName("Kasun Perera")
                .subject("Test Subject")
                .templateName("invalid-template-does-not-exist")
                .templateModel(Map.of("key", "value"))
                .build();

        // When — must NOT throw; service catches and wraps error
        EmailNotificationResponse response = emailService.sendHtmlEmail(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getErrorMessage()).isNotBlank();
        assertThat(response.getRecipientEmail()).isEqualTo("kasun@example.com");
        // mailSender.send() should never be reached when template fails
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // ── Test 5: MailException → graceful FAILED response ─────────────────────

    @Test
    @DisplayName("sendHtmlEmail — MailException from SMTP is caught and returns FAILED")
    void testSendHtmlEmail_MailException_HandlesGracefully() {
        // Given — mock SMTP failure
        doThrow(new org.springframework.mail.MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(MimeMessage.class));

        EmailNotificationRequest request = EmailNotificationRequest.builder()
                .recipientEmail("kasun@example.com")
                .recipientName("Kasun Perera")
                .subject("Booking Confirmed")
                .templateName("booking-confirmed")
                .templateModel(Map.of(
                        "customerName",      "Kasun Perera",
                        "bookingReference",  "RR-BK-9912",
                        "vehicleName",       "Toyota Prius 2021",
                        "startDate",         "2026-11-01",
                        "endDate",           "2026-11-04",
                        "pickupLocation",    "Colombo Central",
                        "totalAmount",       "18,000.00"
                ))
                .build();

        // When
        EmailNotificationResponse response = emailService.sendHtmlEmail(request);

        // Then
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getErrorMessage()).contains("SMTP connection refused");
    }

    // ── Test 6: Async dispatch returns immediately ────────────────────────────

    @Test
    @DisplayName("sendAsyncEmail — dispatches on background Notification-Mail-* thread and returns immediately")
    void testAsyncEmailExecution() throws InterruptedException {
        // Given
        EmailNotificationRequest request = EmailNotificationRequest.builder()
                .recipientEmail("async@example.com")
                .recipientName("Async User")
                .subject("Async Booking Confirmation")
                .templateName("booking-confirmed")
                .templateModel(Map.of(
                        "customerName",      "Async User",
                        "bookingReference",  "RR-BK-ASYNC-001",
                        "vehicleName",       "Honda Civic 2022",
                        "startDate",         "2026-11-05",
                        "endDate",           "2026-11-08",
                        "pickupLocation",    "Kandy Hub",
                        "totalAmount",       "12,000.00"
                ))
                .build();

        // When — sendAsyncEmail must return immediately (non-blocking)
        long before = System.currentTimeMillis();
        emailService.sendAsyncEmail(request);
        long elapsed = System.currentTimeMillis() - before;

        // Then — caller returns in well under 1 second (not waiting for SMTP)
        assertThat(elapsed).isLessThan(1000L);

        // Wait for the background thread to complete (max 5 s), then verify dispatch
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        verify(mailSender, atLeastOnce()).send(any(MimeMessage.class))
                );
    }
}

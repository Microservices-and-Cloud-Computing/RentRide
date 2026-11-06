package com.rentride.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response payload returned after an email notification dispatch attempt.
 *
 * <p>{@code status} will be {@code "SENT"} on successful SMTP delivery,
 * or {@code "FAILED"} if a {@code MailException} or {@code MessagingException}
 * was caught during dispatch. Failed responses include a human-readable
 * {@code errorMessage} for diagnostics without crashing the calling thread.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationResponse {

    /**
     * Unique identifier for this notification dispatch event.
     * Generated as a random UUID string at dispatch time.
     */
    private String notificationId;

    /**
     * The email address the notification was dispatched to.
     */
    private String recipientEmail;

    /**
     * Delivery status: {@code "SENT"} or {@code "FAILED"}.
     */
    private String status;

    /**
     * UTC timestamp of the dispatch attempt.
     */
    private LocalDateTime timestamp;

    /**
     * Human-readable error description when {@code status} is {@code "FAILED"}.
     * {@code null} on successful delivery.
     */
    private String errorMessage;
}


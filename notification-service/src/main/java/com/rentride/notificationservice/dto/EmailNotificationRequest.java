package com.rentride.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Inbound request payload for triggering an HTML transactional email notification.
 *
 * <p>The {@code templateName} maps directly to a Thymeleaf template file located at
 * {@code classpath:/templates/email/<templateName>.html}. All variables required by
 * the template must be provided in {@code templateModel}.
 *
 * <p>Supported template names:
 * <ul>
 *   <li>{@code booking-confirmed} — booking reservation confirmation</li>
 *   <li>{@code rental-completed} — completed rental return summary</li>
 *   <li>{@code payment-receipt} — financial transaction receipt</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationRequest {

    /**
     * Email address of the notification recipient.
     * Must be a syntactically valid RFC-5322 address.
     */
    @NotBlank(message = "recipientEmail must not be blank")
    @Email(message = "recipientEmail must be a valid email address")
    private String recipientEmail;

    /**
     * Display name of the recipient, used inside the email greeting.
     */
    @NotBlank(message = "recipientName must not be blank")
    private String recipientName;

    /**
     * Email subject line. Shown verbatim in mail client previews.
     */
    @NotBlank(message = "subject must not be blank")
    private String subject;

    /**
     * Thymeleaf template file name (without path prefix or {@code .html} suffix).
     * Example: {@code "booking-confirmed"}.
     */
    @NotBlank(message = "templateName must not be blank")
    private String templateName;

    /**
     * Key-value model variables injected into the Thymeleaf template context.
     * Keys must match the variable names referenced inside the HTML template.
     */
    @NotNull(message = "templateModel must not be null")
    private Map<String, Object> templateModel;
}


package com.rentride.notificationservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Externalized configuration properties for RentRide notification mail defaults.
 *
 * <p>Bound from {@code rentride.notification.mail.*} in {@code application.yml}.
 * Supports seamless transition from local development (MailHog/GreenMail) to
 * staging/production SMTP providers (SendGrid, Amazon SES).
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "rentride.notification.mail")
public class MailProperties {

    /**
     * The sender email address for outbound notification emails.
     * Default: {@code no-reply@rentride.com}
     */
    @Email(message = "from-email must be a valid email address")
    @NotBlank(message = "from-email must not be blank")
    private String fromEmail = "no-reply@rentride.com";

    /**
     * The human-readable display name for the sender.
     * Default: {@code RentRide Support}
     */
    @NotBlank(message = "sender-name must not be blank")
    private String senderName = "RentRide Support";
}

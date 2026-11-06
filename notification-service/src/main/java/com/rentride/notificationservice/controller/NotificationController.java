package com.rentride.notificationservice.controller;

import com.rentride.notificationservice.dto.EmailNotificationRequest;
import com.rentride.notificationservice.dto.EmailNotificationResponse;
import com.rentride.notificationservice.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing notification dispatch endpoints.
 *
 * <p>Accepts validated {@link EmailNotificationRequest} payloads and delegates
 * to {@link EmailService} for synchronous or asynchronous SMTP delivery.
 * Mapped under {@code /api/v1/notifications} to align with the API Gateway
 * route {@code lb://NOTIFICATION-SERVICE}.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailService emailService;

    /**
     * Triggers an HTML transactional email notification.
     *
     * <p>Pass the query parameter {@code ?async=true} to dispatch the email
     * on a background thread and return immediately with an {@code "ACCEPTED"}
     * status. Without the parameter (default), the email is sent synchronously
     * and the response reflects the actual SMTP delivery outcome.
     *
     * @param request the validated email notification payload
     * @param async   if {@code true}, dispatch asynchronously (default: {@code false})
     * @return {@link EmailNotificationResponse} with status {@code "SENT"}, {@code "FAILED"},
     *         or {@code "ACCEPTED"} (async mode)
     */
    @PostMapping(
            value = "/send-email",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<EmailNotificationResponse> sendEmail(
            @Valid @RequestBody EmailNotificationRequest request,
            @RequestParam(name = "async", defaultValue = "false") boolean async) {

        log.debug("POST /send-email — recipient='{}', template='{}', async={}",
                request.getRecipientEmail(), request.getTemplateName(), async);

        if (async) {
            emailService.sendAsyncEmail(request);
            EmailNotificationResponse accepted = EmailNotificationResponse.builder()
                    .recipientEmail(request.getRecipientEmail())
                    .status("ACCEPTED")
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
            return ResponseEntity.accepted().body(accepted);
        }

        EmailNotificationResponse response = emailService.sendHtmlEmail(request);
        return ResponseEntity.ok(response);
    }
}


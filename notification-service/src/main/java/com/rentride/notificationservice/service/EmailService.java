package com.rentride.notificationservice.service;

import com.rentride.notificationservice.dto.EmailNotificationRequest;
import com.rentride.notificationservice.dto.EmailNotificationResponse;

/**
 * Contract for the RentRide Notification email dispatch service.
 *
 * <p>Implementations must support both synchronous (request-scoped) and
 * asynchronous (fire-and-forget) email delivery modes.
 */
public interface EmailService {

    /**
     * Renders a Thymeleaf HTML template and dispatches it via SMTP synchronously.
     *
     * <p>Returns a structured {@link EmailNotificationResponse} in both success
     * and failure cases — callers must check {@code response.getStatus()} rather
     * than catching exceptions.
     *
     * @param request the email notification payload including template name and model
     * @return dispatch result with {@code status} of {@code "SENT"} or {@code "FAILED"}
     */
    EmailNotificationResponse sendHtmlEmail(EmailNotificationRequest request);

    /**
     * Dispatches the email asynchronously on the {@code notificationTaskExecutor}
     * thread pool, returning immediately to the caller.
     *
     * <p>Errors are caught and logged internally — no exception propagates to
     * the calling HTTP thread.
     *
     * @param request the email notification payload
     */
    void sendAsyncEmail(EmailNotificationRequest request);
}


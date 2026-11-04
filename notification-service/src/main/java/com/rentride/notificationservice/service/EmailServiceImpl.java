package com.rentride.notificationservice.service;

import com.rentride.notificationservice.config.MailProperties;
import com.rentride.notificationservice.dto.EmailNotificationRequest;
import com.rentride.notificationservice.dto.EmailNotificationResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Default implementation of {@link EmailService}.
 *
 * <p>Renders Thymeleaf HTML templates and dispatches transactional emails via
 * the configured SMTP provider. Supports both blocking (synchronous) and
 * non-blocking (asynchronous) dispatch modes.
 *
 * <p><strong>Template resolution:</strong> template names are resolved to
 * {@code classpath:/templates/email/<templateName>.html} by the Thymeleaf
 * {@link SpringTemplateEngine}. The prefix is configured in {@code application.yml}
 * via {@code spring.thymeleaf.prefix}.
 *
 * <p><strong>Error handling:</strong> both {@link MessagingException} and
 * {@link MailException} are caught and recorded in the returned
 * {@link EmailNotificationResponse} without rethrowing, ensuring callers
 * and async threads are never interrupted by SMTP failures.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final MailProperties mailProperties;

    /**
     * {@inheritDoc}
     *
     * <p>Processing steps:
     * <ol>
     *   <li>Build a Thymeleaf {@link Context} from {@code request.getTemplateModel()}.</li>
     *   <li>Render the HTML body by processing {@code "email/<templateName>"}.</li>
     *   <li>Create a {@link MimeMessage} with {@code MimeMessageHelper} (multipart, UTF-8).</li>
     *   <li>Set recipient, {@code From} header with display name, subject, and HTML body.</li>
     *   <li>Dispatch via {@link JavaMailSender#send(MimeMessage)}.</li>
     * </ol>
     */
    @Override
    public EmailNotificationResponse sendHtmlEmail(EmailNotificationRequest request) {
        String notificationId = UUID.randomUUID().toString();
        log.debug("Dispatching HTML email [id={}] to '{}' using template '{}'",
                notificationId, request.getRecipientEmail(), request.getTemplateName());

        try {
            // 1. Build Thymeleaf context from template model variables
            Context context = new Context();
            if (request.getTemplateModel() != null) {
                context.setVariables(request.getTemplateModel());
            }

            // 2. Render HTML body — template engine resolves prefix + templateName + suffix
            String htmlBody = templateEngine.process(request.getTemplateName(), context);

            // 3. Create MIME message
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // 4. Configure headers
            helper.setTo(request.getRecipientEmail());
            helper.setFrom(mailProperties.getFromEmail(), mailProperties.getSenderName());
            helper.setSubject(request.getSubject());
            helper.setText(htmlBody, true); // true = isHtml

            // 5. Dispatch
            mailSender.send(mimeMessage);

            log.info("Email [id={}] SENT successfully to '{}'", notificationId, request.getRecipientEmail());
            return EmailNotificationResponse.builder()
                    .notificationId(notificationId)
                    .recipientEmail(request.getRecipientEmail())
                    .status("SENT")
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (MessagingException | MailException ex) {
            log.error("Email [id={}] FAILED to '{}' — {}: {}",
                    notificationId, request.getRecipientEmail(), ex.getClass().getSimpleName(), ex.getMessage());
            return EmailNotificationResponse.builder()
                    .notificationId(notificationId)
                    .recipientEmail(request.getRecipientEmail())
                    .status("FAILED")
                    .timestamp(LocalDateTime.now())
                    .errorMessage(ex.getMessage())
                    .build();
        } catch (Exception ex) {
            // Catch template resolution / unexpected errors (e.g., invalid template name)
            log.error("Email [id={}] FAILED — unexpected error for template '{}': {}",
                    notificationId, request.getTemplateName(), ex.getMessage());
            return EmailNotificationResponse.builder()
                    .notificationId(notificationId)
                    .recipientEmail(request.getRecipientEmail())
                    .status("FAILED")
                    .timestamp(LocalDateTime.now())
                    .errorMessage("Template error: " + ex.getMessage())
                    .build();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Dispatched on the {@code notificationTaskExecutor} thread pool.
     * Returns immediately — the caller does not block on SMTP I/O.
     */
    @Override
    @Async("notificationTaskExecutor")
    public void sendAsyncEmail(EmailNotificationRequest request) {
        log.debug("Async email dispatch initiated for '{}' on thread '{}'",
                request.getRecipientEmail(), Thread.currentThread().getName());
        EmailNotificationResponse response = sendHtmlEmail(request);
        log.info("Async email dispatch completed — id={}, status={}", response.getNotificationId(), response.getStatus());
    }
}


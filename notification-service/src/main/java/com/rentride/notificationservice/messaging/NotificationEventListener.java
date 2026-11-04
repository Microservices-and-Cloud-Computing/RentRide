package com.rentride.notificationservice.messaging;

import com.rentride.notificationservice.config.RabbitMQConfig;
import com.rentride.notificationservice.dto.EmailNotificationRequest;
import com.rentride.notificationservice.event.BookingConfirmedEvent;
import com.rentride.notificationservice.event.PaymentProcessedEvent;
import com.rentride.notificationservice.event.RentalLifecycleEvent;
import com.rentride.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ event listener that bridges domain events from Booking-Service and
 * Payment-Service to the email notification pipeline.
 *
 * <p>Each listener method is bound to a dedicated durable queue declared in
 * {@link RabbitMQConfig}. Messages are deserialized as typed Java POJOs via
 * the {@link org.springframework.amqp.support.converter.Jackson2JsonMessageConverter}
 * configured on the listener container factory.
 *
 * <p><strong>Idempotency:</strong> each handler logs its {@code bookingReference}
 * or {@code transactionReference} at {@code INFO} level before dispatch, allowing
 * downstream log aggregation to detect duplicate deliveries.
 *
 * <p><strong>Error isolation:</strong> all exceptions are caught locally so a
 * single bad message never kills the listener thread. After
 * {@code spring.rabbitmq.listener.simple.retry.max-attempts} retries the broker
 * routes the message to the Dead Letter Queue for manual triage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final EmailService emailService;

    // ── Booking Confirmed ────────────────────────────────────────────────────

    /**
     * Handles {@link BookingConfirmedEvent} from {@code notification.booking.confirmed.queue}.
     *
     * <p>Builds a template model and dispatches a booking confirmation email
     * asynchronously to the customer using the {@code booking-confirmed} template.
     *
     * @param event the deserialized booking confirmation domain event
     */
    @RabbitListener(queues = RabbitMQConfig.BOOKING_CONFIRMED_QUEUE)
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        log.info("[AMQP] Received BookingConfirmedEvent — ref={}, customer={}",
                event.getBookingReference(), event.getCustomerEmail());
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("customerName",      event.getCustomerName());
            model.put("bookingReference",  event.getBookingReference());
            model.put("vehicleName",       event.getVehicleName());
            model.put("startDate",         event.getStartDate() != null ? event.getStartDate().toString() : "N/A");
            model.put("endDate",           event.getEndDate() != null ? event.getEndDate().toString() : "N/A");
            model.put("pickupLocation",    event.getPickupLocation());
            model.put("totalAmount",       formatAmount(event.getTotalAmount()));

            EmailNotificationRequest request = EmailNotificationRequest.builder()
                    .recipientEmail(event.getCustomerEmail())
                    .recipientName(event.getCustomerName())
                    .subject("Your RentRide Booking is Confirmed! (Ref: " + event.getBookingReference() + ")")
                    .templateName("booking-confirmed")
                    .templateModel(model)
                    .build();

            emailService.sendAsyncEmail(request);
            log.debug("[AMQP] BookingConfirmedEvent dispatched to email pipeline — ref={}",
                    event.getBookingReference());

        } catch (Exception ex) {
            log.error("[AMQP] Failed to process BookingConfirmedEvent — ref={}: {}",
                    event.getBookingReference(), ex.getMessage(), ex);
            throw ex; // re-throw to trigger AMQP retry / dead-lettering
        }
    }

    // ── Payment Processed ────────────────────────────────────────────────────

    /**
     * Handles {@link PaymentProcessedEvent} from {@code notification.payment.processed.queue}.
     *
     * <p>Routes to one of two email templates:
     * <ul>
     *   <li>{@code payment-receipt} when {@code status == "SUCCESS"}.</li>
     *   <li>A payment failure alert built from the {@code payment-receipt} template
     *       with failure context when {@code status == "FAILED"}.</li>
     * </ul>
     *
     * @param event the deserialized payment processed domain event
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_PROCESSED_QUEUE)
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("[AMQP] Received PaymentProcessedEvent — txn={}, status={}, customer={}",
                event.getTransactionReference(), event.getStatus(), event.getCustomerEmail());
        try {
            if ("SUCCESS".equalsIgnoreCase(event.getStatus())) {
                handlePaymentSuccess(event);
            } else {
                handlePaymentFailure(event);
            }
        } catch (Exception ex) {
            log.error("[AMQP] Failed to process PaymentProcessedEvent — txn={}: {}",
                    event.getTransactionReference(), ex.getMessage(), ex);
            throw ex;
        }
    }

    private void handlePaymentSuccess(PaymentProcessedEvent event) {
        Map<String, Object> model = new HashMap<>();
        model.put("customerName",          event.getCustomerName());
        model.put("transactionReference",  event.getTransactionReference());
        model.put("bookingReference",      event.getBookingReference());
        model.put("paymentMethod",         event.getPaymentMethod());
        model.put("currency",              event.getCurrency());
        model.put("amount",                formatAmount(event.getAmount()));
        model.put("paymentTimestamp",      event.getOccurredOn() != null ? event.getOccurredOn().toString() : "N/A");

        EmailNotificationRequest request = EmailNotificationRequest.builder()
                .recipientEmail(event.getCustomerEmail())
                .recipientName(event.getCustomerName())
                .subject("RentRide Payment Receipt — Txn: " + event.getTransactionReference())
                .templateName("payment-receipt")
                .templateModel(model)
                .build();

        emailService.sendAsyncEmail(request);
        log.debug("[AMQP] Payment SUCCESS email dispatched — txn={}", event.getTransactionReference());
    }

    private void handlePaymentFailure(PaymentProcessedEvent event) {
        Map<String, Object> model = new HashMap<>();
        model.put("customerName",          event.getCustomerName());
        model.put("transactionReference",  event.getTransactionReference());
        model.put("bookingReference",      event.getBookingReference());
        model.put("paymentMethod",         event.getPaymentMethod() != null ? event.getPaymentMethod() : "N/A");
        model.put("currency",              event.getCurrency() != null ? event.getCurrency() : "LKR");
        model.put("amount",                formatAmount(event.getAmount()));
        model.put("paymentTimestamp",      event.getOccurredOn() != null ? event.getOccurredOn().toString() : "N/A");
        // Inject failure context — the payment-receipt template shows this if present
        model.put("paymentStatus",         "FAILED");
        model.put("failureReason",         event.getFailureReason() != null ? event.getFailureReason() : "Payment declined");

        EmailNotificationRequest request = EmailNotificationRequest.builder()
                .recipientEmail(event.getCustomerEmail())
                .recipientName(event.getCustomerName())
                .subject("⚠ RentRide Payment Failed — Please Retry (Ref: " + event.getBookingReference() + ")")
                .templateName("payment-receipt")
                .templateModel(model)
                .build();

        emailService.sendAsyncEmail(request);
        log.warn("[AMQP] Payment FAILED alert email dispatched — txn={}, reason={}",
                event.getTransactionReference(), event.getFailureReason());
    }

    // ── Rental Lifecycle ─────────────────────────────────────────────────────

    /**
     * Handles {@link RentalLifecycleEvent} from {@code notification.rental.lifecycle.queue}.
     *
     * <p>Routing key {@code rental.event.*} covers both {@code rental.event.pickup}
     * and {@code rental.event.return}, discriminated here by {@code event.getEventType()}.
     *
     * @param event the deserialized rental lifecycle domain event
     */
    @RabbitListener(queues = RabbitMQConfig.RENTAL_LIFECYCLE_QUEUE)
    public void handleRentalLifecycle(RentalLifecycleEvent event) {
        log.info("[AMQP] Received RentalLifecycleEvent — type={}, ref={}, customer={}",
                event.getEventType(), event.getBookingReference(), event.getCustomerEmail());
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("customerName",      event.getCustomerName());
            model.put("bookingReference",  event.getBookingReference());
            model.put("vehicleName",       event.getVehicleName() != null ? event.getVehicleName() : "Vehicle");
            model.put("returnDateTime",    event.getOccurredOn() != null ? event.getOccurredOn().toString() : "N/A");
            model.put("finalOdometer",     event.getOdometerReading() != null
                    ? String.format("%.0f", event.getOdometerReading()) : "N/A");
            model.put("returnCondition",   event.getVehicleCondition() != null ? event.getVehicleCondition() : "N/A");
            model.put("additionalCharges", formatAmount(event.getAdditionalCharges()));
            model.put("totalAmount",       formatAmount(event.getAdditionalCharges()));
            model.put("eventType",         event.getEventType());

            String subject;
            if ("PICKUP_COMPLETED".equalsIgnoreCase(event.getEventType())) {
                subject = "Your RentRide Vehicle Pickup is Confirmed! (Ref: " + event.getBookingReference() + ")";
            } else {
                subject = "RentRide Rental Complete — Return Receipt (Ref: " + event.getBookingReference() + ")";
            }

            EmailNotificationRequest request = EmailNotificationRequest.builder()
                    .recipientEmail(event.getCustomerEmail())
                    .recipientName(event.getCustomerName())
                    .subject(subject)
                    .templateName("rental-completed")
                    .templateModel(model)
                    .build();

            emailService.sendAsyncEmail(request);
            log.debug("[AMQP] RentalLifecycleEvent dispatched to email pipeline — type={}, ref={}",
                    event.getEventType(), event.getBookingReference());

        } catch (Exception ex) {
            log.error("[AMQP] Failed to process RentalLifecycleEvent — ref={}: {}",
                    event.getBookingReference(), ex.getMessage(), ex);
            throw ex;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Formats a {@link BigDecimal} amount as a locale-friendly string.
     * Returns {@code "0.00"} for null values.
     */
    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
}


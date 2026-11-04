package com.rentride.bookingservice.service;

import com.rentride.bookingservice.config.RabbitMQProducerConfig;
import com.rentride.bookingservice.event.BookingConfirmedEvent;
import com.rentride.bookingservice.event.RentalLifecycleEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Event publisher for domain events emitted by the Booking-Service.
 *
 * <p>Publishes booking confirmation and rental lifecycle events to the durable
 * topic exchange {@link RabbitMQProducerConfig#RENTRIDE_EXCHANGE}.
 *
 * <p>All dispatches are wrapped in non-blocking try-catch blocks to ensure broker
 * downtimes do not cascade into HTTP 500 errors or rollback primary database transactions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes {@link BookingConfirmedEvent} to {@code rentride.exchange}
     * with routing key {@code booking.event.confirmed}.
     *
     * @param event the booking confirmed domain event
     */
    public void publishBookingConfirmed(BookingConfirmedEvent event) {
        String correlationId = (event != null && event.getBookingReference() != null)
                ? event.getBookingReference()
                : UUID.randomUUID().toString();

        log.info("[AMQP] Publishing BookingConfirmedEvent — ref={}, correlationId={}, customer={}",
                event != null ? event.getBookingReference() : "null", correlationId,
                event != null ? event.getCustomerEmail() : "null");

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQProducerConfig.RENTRIDE_EXCHANGE,
                    RabbitMQProducerConfig.ROUTING_KEY_BOOKING_CONFIRMED,
                    event
            );
            log.info("[AMQP] Successfully published BookingConfirmedEvent — ref={}, correlationId={}",
                    event != null ? event.getBookingReference() : "null", correlationId);
        } catch (Exception ex) {
            log.error("[AMQP] Non-blocking failure publishing BookingConfirmedEvent — ref={}, correlationId={}: {}",
                    event != null ? event.getBookingReference() : "null", correlationId, ex.getMessage(), ex);
        }
    }

    /**
     * Publishes {@link RentalLifecycleEvent} for vehicle pickup to {@code rentride.exchange}
     * with routing key {@code rental.event.pickup}.
     *
     * @param event the rental lifecycle domain event with eventType PICKUP_COMPLETED
     */
    public void publishRentalPickup(RentalLifecycleEvent event) {
        String correlationId = (event != null && event.getBookingReference() != null)
                ? event.getBookingReference() + "-PICKUP"
                : UUID.randomUUID().toString();

        log.info("[AMQP] Publishing RentalLifecycleEvent (PICKUP) — ref={}, correlationId={}, customer={}",
                event != null ? event.getBookingReference() : "null", correlationId,
                event != null ? event.getCustomerEmail() : "null");

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQProducerConfig.RENTRIDE_EXCHANGE,
                    RabbitMQProducerConfig.ROUTING_KEY_RENTAL_PICKUP,
                    event
            );
            log.info("[AMQP] Successfully published RentalLifecycleEvent (PICKUP) — ref={}, correlationId={}",
                    event != null ? event.getBookingReference() : "null", correlationId);
        } catch (Exception ex) {
            log.error("[AMQP] Non-blocking failure publishing RentalLifecycleEvent (PICKUP) — ref={}, correlationId={}: {}",
                    event != null ? event.getBookingReference() : "null", correlationId, ex.getMessage(), ex);
        }
    }

    /**
     * Publishes {@link RentalLifecycleEvent} for vehicle return to {@code rentride.exchange}
     * with routing key {@code rental.event.return}.
     *
     * @param event the rental lifecycle domain event with eventType RETURN_COMPLETED
     */
    public void publishRentalReturn(RentalLifecycleEvent event) {
        String correlationId = (event != null && event.getBookingReference() != null)
                ? event.getBookingReference() + "-RETURN"
                : UUID.randomUUID().toString();

        log.info("[AMQP] Publishing RentalLifecycleEvent (RETURN) — ref={}, correlationId={}, customer={}",
                event != null ? event.getBookingReference() : "null", correlationId,
                event != null ? event.getCustomerEmail() : "null");

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQProducerConfig.RENTRIDE_EXCHANGE,
                    RabbitMQProducerConfig.ROUTING_KEY_RENTAL_RETURN,
                    event
            );
            log.info("[AMQP] Successfully published RentalLifecycleEvent (RETURN) — ref={}, correlationId={}",
                    event != null ? event.getBookingReference() : "null", correlationId);
        } catch (Exception ex) {
            log.error("[AMQP] Non-blocking failure publishing RentalLifecycleEvent (RETURN) — ref={}, correlationId={}: {}",
                    event != null ? event.getBookingReference() : "null", correlationId, ex.getMessage(), ex);
        }
    }
}

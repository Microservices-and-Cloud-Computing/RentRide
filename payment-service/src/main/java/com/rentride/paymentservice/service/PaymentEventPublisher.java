package com.rentride.paymentservice.service;

import com.rentride.paymentservice.config.RabbitMQProducerConfig;
import com.rentride.paymentservice.event.PaymentProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Event publisher for domain events emitted by the Payment-Service.
 *
 * <p>Publishes payment processing outcome events (success and failure) to the durable
 * topic exchange {@link RabbitMQProducerConfig#RENTRIDE_EXCHANGE}.
 *
 * <p>All dispatches are wrapped in non-blocking try-catch blocks to ensure broker
 * downtimes do not cascade into HTTP errors or rollback primary database transactions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes {@link PaymentProcessedEvent} to {@code rentride.exchange}
     * with routing key {@code payment.event.processed}.
     *
     * @param event the payment processed domain event
     */
    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        String correlationId = (event != null && event.getTransactionReference() != null)
                ? event.getTransactionReference()
                : UUID.randomUUID().toString();

        log.info("[AMQP] Publishing PaymentProcessedEvent — txnRef={}, status={}, correlationId={}, customer={}",
                event != null ? event.getTransactionReference() : "null",
                event != null ? event.getStatus() : "null",
                correlationId,
                event != null ? event.getCustomerEmail() : "null");

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQProducerConfig.RENTRIDE_EXCHANGE,
                    RabbitMQProducerConfig.ROUTING_KEY_PAYMENT_PROCESSED,
                    event
            );
            log.info("[AMQP] Successfully published PaymentProcessedEvent — txnRef={}, correlationId={}",
                    event != null ? event.getTransactionReference() : "null", correlationId);
        } catch (Exception ex) {
            log.error("[AMQP] Non-blocking failure publishing PaymentProcessedEvent — txnRef={}, correlationId={}: {}",
                    event != null ? event.getTransactionReference() : "null", correlationId, ex.getMessage(), ex);
        }
    }
}

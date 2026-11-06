package com.rentride.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology and infrastructure configuration for the Notification Microservice.
 *
 * <p><strong>Exchange topology:</strong>
 * <pre>
 *   Topic Exchange: rentride.exchange (durable)
 *     ├── booking.event.confirmed  →  notification.booking.confirmed.queue
 *     ├── payment.event.processed  →  notification.payment.processed.queue
 *     └── rental.event.*           →  notification.rental.lifecycle.queue
 *
 *   Dead Letter Exchange: rentride.dlx (durable)
 *     └── notification.dead-letter.queue  (poison pill sink)
 * </pre>
 *
 * <p><strong>Retry &amp; dead-lettering:</strong> configured via {@code application.yml}
 * ({@code spring.rabbitmq.listener.simple.retry.*}).  After 3 attempts the message
 * is routed to {@code notification.dead-letter.queue} without re-queuing.
 *
 * <p><strong>Message serialization:</strong> all messages use JSON encoding via
 * {@link Jackson2JsonMessageConverter} backed by an {@link ObjectMapper} that
 * registers the {@link JavaTimeModule} for {@code LocalDate}/{@code LocalDateTime}
 * support.
 */
@Configuration
public class RabbitMQConfig {

    // ── Exchange names ───────────────────────────────────────────────────────

    public static final String RENTRIDE_EXCHANGE     = "rentride.exchange";
    public static final String DEAD_LETTER_EXCHANGE  = "rentride.dlx";

    // ── Queue names ──────────────────────────────────────────────────────────

    public static final String BOOKING_CONFIRMED_QUEUE   = "notification.booking.confirmed.queue";
    public static final String PAYMENT_PROCESSED_QUEUE   = "notification.payment.processed.queue";
    public static final String RENTAL_LIFECYCLE_QUEUE    = "notification.rental.lifecycle.queue";
    public static final String DEAD_LETTER_QUEUE         = "notification.dead-letter.queue";

    // ── Routing keys ─────────────────────────────────────────────────────────

    public static final String ROUTING_KEY_BOOKING_CONFIRMED  = "booking.event.confirmed";
    public static final String ROUTING_KEY_PAYMENT_PROCESSED  = "payment.event.processed";
    public static final String ROUTING_KEY_RENTAL_LIFECYCLE   = "rental.event.*";

    // ── Exchanges ────────────────────────────────────────────────────────────

    /**
     * Main durable topic exchange for all RentRide domain events.
     * Booking-Service and Payment-Service publish to this exchange.
     */
    @Bean
    public TopicExchange rentrideExchange() {
        return new TopicExchange(RENTRIDE_EXCHANGE, true, false);
    }

    /**
     * Dead Letter Exchange — receives messages after retry exhaustion.
     */
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    // ── Queues ───────────────────────────────────────────────────────────────

    /**
     * Durable queue for booking confirmation events.
     * Dead-letters to {@code rentride.dlx} after retry exhaustion.
     */
    @Bean
    public Queue bookingConfirmedQueue() {
        return QueueBuilder.durable(BOOKING_CONFIRMED_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq.booking.confirmed")
                .build();
    }

    /**
     * Durable queue for payment processed events (success and failure).
     */
    @Bean
    public Queue paymentProcessedQueue() {
        return QueueBuilder.durable(PAYMENT_PROCESSED_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq.payment.processed")
                .build();
    }

    /**
     * Durable queue for rental lifecycle events (pickup &amp; return).
     * The wildcard routing key {@code rental.event.*} covers both sub-events.
     */
    @Bean
    public Queue rentalLifecycleQueue() {
        return QueueBuilder.durable(RENTAL_LIFECYCLE_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq.rental.lifecycle")
                .build();
    }

    /**
     * Dead Letter Queue — sink for unprocessable / poison-pill messages.
     * Inspect this queue in the RabbitMQ Management UI to triage failures.
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    // ── Bindings ─────────────────────────────────────────────────────────────

    @Bean
    public Binding bookingConfirmedBinding() {
        return BindingBuilder.bind(bookingConfirmedQueue())
                .to(rentrideExchange())
                .with(ROUTING_KEY_BOOKING_CONFIRMED);
    }

    @Bean
    public Binding paymentProcessedBinding() {
        return BindingBuilder.bind(paymentProcessedQueue())
                .to(rentrideExchange())
                .with(ROUTING_KEY_PAYMENT_PROCESSED);
    }

    @Bean
    public Binding rentalLifecycleBinding() {
        return BindingBuilder.bind(rentalLifecycleQueue())
                .to(rentrideExchange())
                .with(ROUTING_KEY_RENTAL_LIFECYCLE);
    }

    // ── Message Converter ────────────────────────────────────────────────────

    /**
     * Jackson-based JSON message converter.
     *
     * <p>Registers {@link JavaTimeModule} so that {@code LocalDate} and
     * {@code LocalDateTime} fields in domain events are serialized/deserialized
     * as ISO-8601 strings rather than numeric arrays.
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    /**
     * Override the default {@link RabbitTemplate} to use the JSON converter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter());
        return template;
    }

    /**
     * Override the listener container factory to use the JSON converter,
     * ensuring {@code @RabbitListener} methods receive typed POJOs.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter());
        return factory;
    }
}


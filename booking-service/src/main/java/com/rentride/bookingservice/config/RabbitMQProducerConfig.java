package com.rentride.bookingservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ producer configuration for Booking-Service.
 *
 * <p>Declares the shared durable topic exchange {@code rentride.exchange},
 * routing key constants for booking and rental lifecycle events, and configures
 * {@link RabbitTemplate} with JSON serialization supporting Java 8 date/time types
 * as well as publisher confirms and return callbacks.
 */
@Slf4j
@Configuration
public class RabbitMQProducerConfig {

    public static final String RENTRIDE_EXCHANGE = "rentride.exchange";

    public static final String ROUTING_KEY_BOOKING_CONFIRMED = "booking.event.confirmed";
    public static final String ROUTING_KEY_RENTAL_PICKUP     = "rental.event.pickup";
    public static final String ROUTING_KEY_RENTAL_RETURN     = "rental.event.return";
    public static final String ROUTING_KEY_PAYMENT_PROCESSED = "payment.event.processed";

    /**
     * Shared durable topic exchange for all RentRide domain events.
     */
    @Bean
    public TopicExchange rentrideExchange() {
        return new TopicExchange(RENTRIDE_EXCHANGE, true, false);
    }

    /**
     * Jackson JSON message converter with JSR-310 JavaTimeModule registered,
     * ensuring {@code LocalDate} and {@code LocalDateTime} are serialized as ISO-8601 strings.
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    /**
     * Configured {@link RabbitTemplate} with JSON message converter and
     * publisher confirm and return logging callbacks.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter());
        template.setConfirmCallback((correlationData, ack, cause) -> {
            String corrId = (correlationData != null) ? correlationData.getId() : "null";
            if (ack) {
                log.debug("[AMQP-CONFIRM] Broker ACK received for correlationId={}", corrId);
            } else {
                log.error("[AMQP-CONFIRM] Broker NACK received for correlationId={}, cause={}", corrId, cause);
            }
        });
        template.setReturnsCallback(returned -> {
            log.warn("[AMQP-RETURN] Message returned from exchange={}: replyCode={}, replyText={}, routingKey={}",
                    returned.getExchange(), returned.getReplyCode(), returned.getReplyText(), returned.getRoutingKey());
        });
        return template;
    }
}

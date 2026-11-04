package com.rentride.e2e.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rentride.e2e.event.BookingConfirmedEvent;
import com.rentride.e2e.event.PaymentProcessedEvent;
import com.rentride.e2e.event.RentalLifecycleEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Configuration
public class RabbitMQTestConfig {

    public static final String RENTRIDE_EXCHANGE = "rentride.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "rentride.dlx";

    public static final String BOOKING_CONFIRMED_QUEUE = "notification.booking.confirmed.queue";
    public static final String PAYMENT_PROCESSED_QUEUE = "notification.payment.processed.queue";
    public static final String RENTAL_LIFECYCLE_QUEUE = "notification.rental.lifecycle.queue";
    public static final String DEAD_LETTER_QUEUE = "notification.dead-letter.queue";

    public static final String ROUTING_KEY_BOOKING_CONFIRMED = "booking.event.confirmed";
    public static final String ROUTING_KEY_PAYMENT_PROCESSED = "payment.event.processed";
    public static final String ROUTING_KEY_RENTAL_LIFECYCLE = "rental.event.*";

    @Bean
    public TopicExchange rentrideExchange() {
        return new TopicExchange(RENTRIDE_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue bookingConfirmedQueue() {
        return QueueBuilder.durable(BOOKING_CONFIRMED_QUEUE).build();
    }

    @Bean
    public Queue paymentProcessedQueue() {
        return QueueBuilder.durable(PAYMENT_PROCESSED_QUEUE).build();
    }

    @Bean
    public Queue rentalLifecycleQueue() {
        return QueueBuilder.durable(RENTAL_LIFECYCLE_QUEUE).build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding bookingConfirmedBinding(Queue bookingConfirmedQueue, TopicExchange rentrideExchange) {
        return BindingBuilder.bind(bookingConfirmedQueue).to(rentrideExchange).with(ROUTING_KEY_BOOKING_CONFIRMED);
    }

    @Bean
    public Binding paymentProcessedBinding(Queue paymentProcessedQueue, TopicExchange rentrideExchange) {
        return BindingBuilder.bind(paymentProcessedQueue).to(rentrideExchange).with(ROUTING_KEY_PAYMENT_PROCESSED);
    }

    @Bean
    public Binding rentalLifecycleBinding(Queue rentalLifecycleQueue, TopicExchange rentrideExchange) {
        return BindingBuilder.bind(rentalLifecycleQueue).to(rentrideExchange).with(ROUTING_KEY_RENTAL_LIFECYCLE);
    }

    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    /**
     * In-memory test event listener capturing published AMQP domain events
     * for verification by Awaitility assertions in integration tests.
     */
    @Component
    @Getter
    public static class TestEventCollector {

        private final List<BookingConfirmedEvent> bookingConfirmedEvents = new CopyOnWriteArrayList<>();
        private final List<PaymentProcessedEvent> paymentProcessedEvents = new CopyOnWriteArrayList<>();
        private final List<RentalLifecycleEvent> rentalLifecycleEvents = new CopyOnWriteArrayList<>();

        @RabbitListener(queues = BOOKING_CONFIRMED_QUEUE)
        public void onBookingConfirmed(BookingConfirmedEvent event) {
            log.info("TestEventCollector received BookingConfirmedEvent: bookingRef={}, customer={}",
                    event.getBookingReference(), event.getCustomerEmail());
            bookingConfirmedEvents.add(event);
        }

        @RabbitListener(queues = PAYMENT_PROCESSED_QUEUE)
        public void onPaymentProcessed(PaymentProcessedEvent event) {
            log.info("TestEventCollector received PaymentProcessedEvent: txnRef={}, status={}, amount={}",
                    event.getTransactionReference(), event.getStatus(), event.getAmount());
            paymentProcessedEvents.add(event);
        }

        @RabbitListener(queues = RENTAL_LIFECYCLE_QUEUE)
        public void onRentalLifecycle(RentalLifecycleEvent event) {
            log.info("TestEventCollector received RentalLifecycleEvent: bookingRef={}, type={}",
                    event.getBookingReference(), event.getEventType());
            rentalLifecycleEvents.add(event);
        }

        public void recordBookingConfirmed(BookingConfirmedEvent event) {
            bookingConfirmedEvents.add(event);
        }

        public void recordPaymentProcessed(PaymentProcessedEvent event) {
            paymentProcessedEvents.add(event);
        }

        public void recordRentalLifecycle(RentalLifecycleEvent event) {
            rentalLifecycleEvents.add(event);
        }

        public void clear() {
            bookingConfirmedEvents.clear();
            paymentProcessedEvents.clear();
            rentalLifecycleEvents.clear();
        }
    }
}


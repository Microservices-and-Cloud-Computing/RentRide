package com.rentride.paymentservice.service;

import com.rentride.paymentservice.config.RabbitMQProducerConfig;
import com.rentride.paymentservice.event.PaymentProcessedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit & Mock Verification Tests for {@link PaymentEventPublisher}.
 *
 * <p>Validates domain event serialization dispatch to RabbitMQ exchange
 * and fault-tolerance against broker failures.
 */
@SpringBootTest
@ActiveProfiles("test")
class PaymentEventPublisherTest {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PaymentEventPublisher paymentEventPublisher;

    private PaymentProcessedEvent successEvent;
    private PaymentProcessedEvent failedEvent;

    @BeforeEach
    void setUp() {
        successEvent = PaymentProcessedEvent.builder()
                .transactionReference("TXN-20261104-SUCCESS01")
                .bookingReference("RR-BK-20261104-001")
                .customerEmail("kasun@rentride.com")
                .customerName("Kasun Perera")
                .amount(new BigDecimal("24000.00"))
                .currency("LKR")
                .paymentMethod("CREDIT_CARD")
                .status("SUCCESS")
                .receiptUrl("http://localhost:8084/receipts/TXN-20261104-SUCCESS01")
                .occurredOn(LocalDateTime.now())
                .build();

        failedEvent = PaymentProcessedEvent.builder()
                .transactionReference("TXN-20261104-FAIL01")
                .bookingReference("RR-BK-20261104-001")
                .customerEmail("kasun@rentride.com")
                .customerName("Kasun Perera")
                .amount(new BigDecimal("24000.00"))
                .currency("LKR")
                .paymentMethod("CREDIT_CARD")
                .status("FAILED")
                .failureReason("Insufficient funds")
                .occurredOn(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("testPublishPaymentProcessed_SuccessAndFailure: Dispatches both SUCCESS and FAILED events to rentride.exchange with payment.event.processed")
    void testPublishPaymentProcessed_SuccessAndFailure() {
        // 1. Publish SUCCESS event
        paymentEventPublisher.publishPaymentProcessed(successEvent);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQProducerConfig.RENTRIDE_EXCHANGE),
                eq(RabbitMQProducerConfig.ROUTING_KEY_PAYMENT_PROCESSED),
                eq(successEvent)
        );

        // 2. Publish FAILED event
        paymentEventPublisher.publishPaymentProcessed(failedEvent);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQProducerConfig.RENTRIDE_EXCHANGE),
                eq(RabbitMQProducerConfig.ROUTING_KEY_PAYMENT_PROCESSED),
                eq(failedEvent)
        );
    }

    @Test
    @DisplayName("testBrokerFailure_DoesNotCrashPrimaryTransaction: AmqpException handled gracefully without throwing unhandled exception")
    void testBrokerFailure_DoesNotCrashPrimaryTransaction() {
        doThrow(new AmqpException("Broker cluster connection timed out"))
                .when(rabbitTemplate)
                .convertAndSend(eq(RabbitMQProducerConfig.RENTRIDE_EXCHANGE), eq(RabbitMQProducerConfig.ROUTING_KEY_PAYMENT_PROCESSED), any(PaymentProcessedEvent.class));

        assertThatCode(() -> paymentEventPublisher.publishPaymentProcessed(successEvent))
                .doesNotThrowAnyException();
    }
}

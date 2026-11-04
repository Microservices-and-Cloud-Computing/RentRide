package com.rentride.bookingservice.service;

import com.rentride.bookingservice.config.RabbitMQProducerConfig;
import com.rentride.bookingservice.event.BookingConfirmedEvent;
import com.rentride.bookingservice.event.RentalLifecycleEvent;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit & Mock Verification Tests for {@link BookingEventPublisher}.
 *
 * <p>Validates domain event serialization dispatch to RabbitMQ exchange
 * and fault-tolerance against broker failures.
 */
@SpringBootTest
@ActiveProfiles("test")
class BookingEventPublisherTest {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private BookingEventPublisher bookingEventPublisher;

    private BookingConfirmedEvent sampleConfirmedEvent;
    private RentalLifecycleEvent samplePickupEvent;
    private RentalLifecycleEvent sampleReturnEvent;

    @BeforeEach
    void setUp() {
        sampleConfirmedEvent = BookingConfirmedEvent.builder()
                .bookingReference("RR-BK-20261104-001")
                .customerEmail("kasun@rentride.com")
                .customerName("Kasun Perera")
                .providerEmail("provider@rentride.com")
                .vehicleName("Toyota Prius 2023")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(4))
                .totalAmount(new BigDecimal("24000.00"))
                .pickupLocation("Colombo Central Branch")
                .occurredOn(LocalDateTime.now())
                .build();

        samplePickupEvent = RentalLifecycleEvent.builder()
                .bookingReference("RR-BK-20261104-001")
                .customerEmail("kasun@rentride.com")
                .customerName("Kasun Perera")
                .vehicleName("Toyota Prius 2023")
                .eventType("PICKUP_COMPLETED")
                .odometerReading(35000.0)
                .vehicleCondition("GOOD")
                .additionalCharges(BigDecimal.ZERO)
                .occurredOn(LocalDateTime.now())
                .build();

        sampleReturnEvent = RentalLifecycleEvent.builder()
                .bookingReference("RR-BK-20261104-001")
                .customerEmail("kasun@rentride.com")
                .customerName("Kasun Perera")
                .vehicleName("Toyota Prius 2023")
                .eventType("RETURN_COMPLETED")
                .odometerReading(35400.0)
                .vehicleCondition("GOOD")
                .additionalCharges(BigDecimal.ZERO)
                .occurredOn(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("testPublishBookingConfirmed_SendsMessageToRabbitTemplate: Dispatches to rentride.exchange with booking.event.confirmed")
    void testPublishBookingConfirmed_SendsMessageToRabbitTemplate() {
        bookingEventPublisher.publishBookingConfirmed(sampleConfirmedEvent);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQProducerConfig.RENTRIDE_EXCHANGE),
                eq(RabbitMQProducerConfig.ROUTING_KEY_BOOKING_CONFIRMED),
                any(BookingConfirmedEvent.class)
        );
    }

    @Test
    @DisplayName("testPublishRentalPickup_SendsMessageToRabbitTemplate: Dispatches to rentride.exchange with rental.event.pickup")
    void testPublishRentalPickup_SendsMessageToRabbitTemplate() {
        bookingEventPublisher.publishRentalPickup(samplePickupEvent);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQProducerConfig.RENTRIDE_EXCHANGE),
                eq(RabbitMQProducerConfig.ROUTING_KEY_RENTAL_PICKUP),
                any(RentalLifecycleEvent.class)
        );
    }

    @Test
    @DisplayName("testPublishRentalReturn_SendsMessageToRabbitTemplate: Dispatches to rentride.exchange with rental.event.return")
    void testPublishRentalReturn_SendsMessageToRabbitTemplate() {
        bookingEventPublisher.publishRentalReturn(sampleReturnEvent);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQProducerConfig.RENTRIDE_EXCHANGE),
                eq(RabbitMQProducerConfig.ROUTING_KEY_RENTAL_RETURN),
                any(RentalLifecycleEvent.class)
        );
    }

    @Test
    @DisplayName("testBrokerFailure_DoesNotCrashPrimaryTransaction: AmqpException handled gracefully without throwing")
    void testBrokerFailure_DoesNotCrashPrimaryTransaction() {
        doThrow(new AmqpException("Simulated broker network failure"))
                .when(rabbitTemplate)
                .convertAndSend(eq(RabbitMQProducerConfig.RENTRIDE_EXCHANGE), eq(RabbitMQProducerConfig.ROUTING_KEY_BOOKING_CONFIRMED), any(BookingConfirmedEvent.class));

        assertThatCode(() -> bookingEventPublisher.publishBookingConfirmed(sampleConfirmedEvent))
                .doesNotThrowAnyException();
    }
}

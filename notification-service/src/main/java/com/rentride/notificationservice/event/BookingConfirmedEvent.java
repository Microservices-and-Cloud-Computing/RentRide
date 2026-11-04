package com.rentride.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain event emitted by the Booking-Service when a vehicle reservation
 * transitions to the {@code CONFIRMED} state.
 *
 * <p>Consumed from {@code notification.booking.confirmed.queue} via RabbitMQ.
 * The notification-service uses this event to dispatch a booking confirmation
 * email to both the customer and the fleet provider.
 *
 * <p>JSON deserialization relies on Jackson's {@code JavaTimeModule} registered
 * in {@link com.rentride.notificationservice.config.RabbitMQConfig}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmedEvent {

    /** Unique reservation reference (e.g., {@code RR-BK-20261018-001}). */
    private String bookingReference;

    /** Email address of the renting customer — primary notification recipient. */
    private String customerEmail;

    /** Display name of the customer for personalised email greeting. */
    private String customerName;

    /** Fleet provider email address for provider-side notification. */
    private String providerEmail;

    /** Human-readable vehicle description (make, model, year). */
    private String vehicleName;

    /** Rental commencement date. */
    private LocalDate startDate;

    /** Rental end / scheduled return date. */
    private LocalDate endDate;

    /** Agreed total rental amount in LKR. */
    private BigDecimal totalAmount;

    /** Vehicle pickup branch or location name. */
    private String pickupLocation;

    /** Wall-clock timestamp when the booking confirmation event was raised. */
    private LocalDateTime occurredOn;
}


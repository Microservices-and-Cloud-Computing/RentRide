package com.rentride.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain event emitted by the Booking-Service when a rental vehicle
 * is physically picked up or returned by the customer.
 *
 * <p>Consumed from {@code notification.rental.lifecycle.queue} via RabbitMQ
 * using the wildcard routing key {@code rental.event.*}, which matches both
 * {@code rental.event.pickup} and {@code rental.event.return}.
 *
 * <p>The notification-service dispatches:
 * <ul>
 *   <li>A pickup confirmation when {@code eventType == "PICKUP_COMPLETED"}.</li>
 *   <li>A final return receipt when {@code eventType == "RETURN_COMPLETED"},
 *       including odometer delta, vehicle condition, and any additional charges.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalLifecycleEvent {

    /** Associated booking reference. */
    private String bookingReference;

    /** Email address of the customer. */
    private String customerEmail;

    /** Display name of the customer. */
    private String customerName;

    /** Human-readable vehicle description. */
    private String vehicleName;

    /**
     * Lifecycle phase discriminator.
     * <ul>
     *   <li>{@code PICKUP_COMPLETED} — vehicle handed over to customer.</li>
     *   <li>{@code RETURN_COMPLETED} — vehicle returned to fleet.</li>
     * </ul>
     */
    private String eventType;

    /** Odometer reading at the time of the lifecycle event (kilometres). */
    private Double odometerReading;

    /**
     * Assessed vehicle condition at pickup or return
     * (e.g., {@code "Good"}, {@code "Minor scratches"}, {@code "Fuel short"}).
     */
    private String vehicleCondition;

    /**
     * Any additional charges levied at return (late fees, damage, fuel top-up).
     * Zero or {@code null} for pickup events.
     */
    private BigDecimal additionalCharges;

    /** Wall-clock timestamp when this lifecycle event was raised. */
    private LocalDateTime occurredOn;
}


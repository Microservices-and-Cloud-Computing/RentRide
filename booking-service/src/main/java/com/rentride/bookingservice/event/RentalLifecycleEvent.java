package com.rentride.bookingservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain event emitted by Booking-Service when a rental vehicle
 * is physically picked up or returned by the customer.
 *
 * <p>Published to {@code rentride.exchange} with routing key:
 * <ul>
 *   <li>{@code rental.event.pickup} for pickup completion</li>
 *   <li>{@code rental.event.return} for return completion</li>
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
     * (e.g., {@code "GOOD"}, {@code "FAIR"}, {@code "DAMAGED"}).
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

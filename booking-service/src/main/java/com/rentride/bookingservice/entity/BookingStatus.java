package com.rentride.bookingservice.entity;

/**
 * Enumeration representing the reservation and rental lifecycle statuses of a booking.
 *
 * <ul>
 *   <li>{@link #PENDING_PAYMENT} — Initial state upon vehicle reservation request; awaiting payment settlement.</li>
 *   <li>{@link #CONFIRMED}       — Payment verified; vehicle is actively reserved for the agreed rental dates.</li>
 *   <li>{@link #ACTIVE_RENTAL}   — Customer has completed vehicle pickup; rental is actively in progress.</li>
 *   <li>{@link #COMPLETED}       — Vehicle returned, inspected, and the reservation lifecycle concluded.</li>
 *   <li>{@link #CANCELLED}       — Cancelled by customer/provider or auto-expired prior to payment settlement.</li>
 * </ul>
 */
public enum BookingStatus {

    /**
     * Initial state upon vehicle reservation request; awaiting payment settlement.
     */
    PENDING_PAYMENT,

    /**
     * Payment verified; vehicle is actively reserved for the agreed rental dates.
     */
    CONFIRMED,

    /**
     * Customer has completed vehicle pickup; rental is actively in progress.
     */
    ACTIVE_RENTAL,

    /**
     * Vehicle returned, inspected, and the reservation lifecycle concluded.
     */
    COMPLETED,

    /**
     * Cancelled by customer/provider or auto-expired prior to payment settlement.
     */
    CANCELLED
}


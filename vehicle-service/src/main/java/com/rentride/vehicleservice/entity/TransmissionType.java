package com.rentride.vehicleservice.entity;

/**
 * Enumeration representing the gearbox transmission type of a vehicle.
 *
 * <ul>
 *   <li>{@link #MANUAL}    — Manual gearbox requiring driver-operated clutch and manual gear selection.</li>
 *   <li>{@link #AUTOMATIC} — Automatic gearbox (torque-converter, dual-clutch, or CVT) handling gear ratios automatically.</li>
 * </ul>
 */
public enum TransmissionType {
    MANUAL,
    AUTOMATIC
}

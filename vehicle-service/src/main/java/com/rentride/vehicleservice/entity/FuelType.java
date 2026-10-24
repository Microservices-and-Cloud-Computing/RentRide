package com.rentride.vehicleservice.entity;

/**
 * Enumeration representing the powertrain fuel source and energy type of a vehicle.
 *
 * <ul>
 *   <li>{@link #PETROL}   — Internal combustion engine running on gasoline/petrol.</li>
 *   <li>{@link #DIESEL}   — Compression-ignition internal combustion engine running on diesel.</li>
 *   <li>{@link #HYBRID}   — Combined internal combustion engine and electric propulsion system.</li>
 *   <li>{@link #ELECTRIC} — Pure battery electric vehicle (BEV) with zero direct emissions.</li>
 * </ul>
 */
public enum FuelType {
    PETROL,
    DIESEL,
    HYBRID,
    ELECTRIC
}

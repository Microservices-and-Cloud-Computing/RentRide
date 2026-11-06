package com.rentride.vehicleservice.entity;

/**
 * Enumeration representing the operational and inventory status of a vehicle.
 *
 * <ul>
 *   <li>{@link #AVAILABLE}       — Ready and available for customer booking and dispatch.</li>
 *   <li>{@link #RENTED}          — Currently dispatched and active in an ongoing customer rental agreement.</li>
 *   <li>{@link #MAINTENANCE}     — Under scheduled routine servicing, mechanical inspection, or repairs.</li>
 *   <li>{@link #DECOMMISSIONED}  — Permanently retired from the platform fleet.</li>
 * </ul>
 */
public enum VehicleStatus {
    AVAILABLE,
    RENTED,
    MAINTENANCE,
    DECOMMISSIONED
}

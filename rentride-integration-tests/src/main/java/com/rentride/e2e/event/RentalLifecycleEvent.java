package com.rentride.e2e.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Shared domain event emitted during rental lifecycle state transitions (PICKUP_COMPLETED, RETURN_COMPLETED).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalLifecycleEvent {

    private String bookingReference;
    private String customerEmail;
    private String customerName;
    private String vehicleName;
    private String eventType;
    private Double odometerReading;
    private Integer fuelLevelPercent;
    private String vehicleCondition;
    private BigDecimal additionalCharges;
    private LocalDateTime occurredOn;
}


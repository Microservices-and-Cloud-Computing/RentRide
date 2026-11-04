package com.rentride.e2e.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Shared domain event emitted when a vehicle reservation is confirmed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmedEvent {

    private String bookingReference;
    private String customerEmail;
    private String customerName;
    private String providerEmail;
    private String vehicleName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalAmount;
    private String pickupLocation;
    private LocalDateTime occurredOn;
}


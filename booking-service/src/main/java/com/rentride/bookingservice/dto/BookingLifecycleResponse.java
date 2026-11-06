package com.rentride.bookingservice.dto;

import com.rentride.bookingservice.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representation of a booking's lifecycle transition state (e.g., Pickup or Return).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingLifecycleResponse {

    private Long id;
    private String bookingReference;
    private Long customerId;
    private Long providerId;
    private Long vehicleId;
    private BookingStatus status;
    private LocalDateTime actualPickupTime;
    private LocalDateTime actualReturnDate;
    private String vehicleStatusSummary;
    private Double startOdometer;
    private Double endOdometer;
    private Integer startFuelPercent;
    private Integer endFuelPercent;
    private BigDecimal totalCost;
    private BigDecimal additionalCharges;
    private BigDecimal finalAmountPaid;
    private String pickupNotes;
    private String returnNotes;
}


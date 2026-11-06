package com.rentride.bookingservice.dto;

import com.rentride.bookingservice.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Detailed representation of a vehicle reservation returned to API clients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Long id;
    private String bookingReference;
    private Long customerId;
    private Long providerId;
    private Long vehicleId;
    private VehicleDetails vehicleDetails;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private BigDecimal dailyRate;
    private BigDecimal totalCost;
    private BookingStatus status;
    private String pickupNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Nested DTO capturing vehicle specifications associated with the booking.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleDetails {
        private String make;
        private String model;
        private String licensePlateNumber;
        private String categoryName;
        private String location;
    }
}


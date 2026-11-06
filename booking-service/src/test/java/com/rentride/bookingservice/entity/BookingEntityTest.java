package com.rentride.bookingservice.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Booking} entity and {@link BookingStatus} enum.
 */
class BookingEntityTest {

    @Test
    @DisplayName("Should default status to PENDING_PAYMENT and additionalCharges to ZERO when built via Lombok Builder")
    void shouldDefaultStatusToPendingPayment() {
        Booking booking = Booking.builder()
                .bookingReference("RR-BK-20261025-001122")
                .customerId(1L)
                .providerId(2L)
                .vehicleId(3L)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .totalDays(4)
                .dailyRate(new BigDecimal("6000.00"))
                .totalCost(new BigDecimal("24000.00"))
                .build();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.getAdditionalCharges()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should correctly set and retrieve all booking properties including pickup and return metrics")
    void shouldSetAndGetProperties() {
        Booking booking = new Booking();
        booking.setId(10L);
        booking.setBookingReference("RR-BK-REF-10");
        booking.setCustomerId(50L);
        booking.setProviderId(60L);
        booking.setVehicleId(70L);
        booking.setStartDate(LocalDate.of(2026, 11, 1));
        booking.setEndDate(LocalDate.of(2026, 11, 5));
        booking.setTotalDays(5);
        booking.setDailyRate(new BigDecimal("5000.00"));
        booking.setTotalCost(new BigDecimal("25000.00"));
        booking.setStatus(BookingStatus.ACTIVE_RENTAL);
        booking.setPickupNotes("Key handed over with full fuel");
        booking.setReturnNotes("Vehicle checked, no damages");
        LocalDateTime pickupTime = LocalDateTime.of(2026, 11, 1, 10, 0);
        LocalDateTime returnTime = LocalDateTime.of(2026, 11, 5, 17, 30);
        booking.setActualPickupTime(pickupTime);
        booking.setActualReturnDate(returnTime);
        booking.setStartOdometer(45000.0);
        booking.setEndOdometer(45650.0);
        booking.setStartFuelPercent(100);
        booking.setEndFuelPercent(95);
        booking.setAdditionalCharges(new BigDecimal("1500.00"));

        assertThat(booking.getId()).isEqualTo(10L);
        assertThat(booking.getBookingReference()).isEqualTo("RR-BK-REF-10");
        assertThat(booking.getCustomerId()).isEqualTo(50L);
        assertThat(booking.getProviderId()).isEqualTo(60L);
        assertThat(booking.getVehicleId()).isEqualTo(70L);
        assertThat(booking.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(booking.getEndDate()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(booking.getTotalDays()).isEqualTo(5);
        assertThat(booking.getDailyRate()).isEqualByComparingTo("5000.00");
        assertThat(booking.getTotalCost()).isEqualByComparingTo("25000.00");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACTIVE_RENTAL);
        assertThat(booking.getPickupNotes()).isEqualTo("Key handed over with full fuel");
        assertThat(booking.getReturnNotes()).isEqualTo("Vehicle checked, no damages");
        assertThat(booking.getActualPickupTime()).isEqualTo(pickupTime);
        assertThat(booking.getActualReturnDate()).isEqualTo(returnTime);
        assertThat(booking.getStartOdometer()).isEqualTo(45000.0);
        assertThat(booking.getEndOdometer()).isEqualTo(45650.0);
        assertThat(booking.getStartFuelPercent()).isEqualTo(100);
        assertThat(booking.getEndFuelPercent()).isEqualTo(95);
        assertThat(booking.getAdditionalCharges()).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("Should verify all expected lifecycle values in BookingStatus enum")
    void shouldContainAllLifecycleStatuses() {
        BookingStatus[] statuses = BookingStatus.values();

        assertThat(statuses).containsExactly(
                BookingStatus.PENDING_PAYMENT,
                BookingStatus.CONFIRMED,
                BookingStatus.ACTIVE_RENTAL,
                BookingStatus.COMPLETED,
                BookingStatus.CANCELLED
        );
    }
}

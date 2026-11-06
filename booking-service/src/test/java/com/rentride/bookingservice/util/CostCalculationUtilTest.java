package com.rentride.bookingservice.util;

import com.rentride.bookingservice.exception.InvalidBookingDatesException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CostCalculationUtilTest {

    @Test
    @DisplayName("Should count same-day rental as 1 day and compute 5000.00 total cost")
    void testCostCalculation_SameDayRental_CountsAsOneDay() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 1);
        BigDecimal dailyRate = new BigDecimal("5000.00");

        int totalDays = CostCalculationUtil.calculateRentalDays(start, end);
        BigDecimal totalCost = CostCalculationUtil.calculateTotalCost(totalDays, dailyRate);

        assertThat(totalDays).isEqualTo(1);
        assertThat(totalCost).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(totalCost.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should accurately calculate multi-day rental (4 days @ 8,500.00 = 34,000.00)")
    void testCostCalculation_MultiDayRental_CalculatesAccurately() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 5);
        BigDecimal dailyRate = new BigDecimal("8500.00");

        int totalDays = CostCalculationUtil.calculateRentalDays(start, end);
        BigDecimal totalCost = CostCalculationUtil.calculateTotalCost(totalDays, dailyRate);

        assertThat(totalDays).isEqualTo(4);
        assertThat(totalCost).isEqualByComparingTo(new BigDecimal("34000.00"));
        assertThat(totalCost.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should throw InvalidBookingDatesException when end date is before start date")
    void testCalculateRentalDays_EndDateBeforeStartDate_ThrowsException() {
        LocalDate start = LocalDate.of(2026, 11, 5);
        LocalDate end = LocalDate.of(2026, 11, 1);

        assertThatThrownBy(() -> CostCalculationUtil.calculateRentalDays(start, end))
                .isInstanceOf(InvalidBookingDatesException.class)
                .hasMessageContaining("cannot be before start date");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when dates are null")
    void testCalculateRentalDays_NullDates_ThrowsException() {
        assertThatThrownBy(() -> CostCalculationUtil.calculateRentalDays(null, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> CostCalculationUtil.calculateRentalDays(LocalDate.now(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when rate is null or days <= 0")
    void testCalculateTotalCost_InvalidArguments_ThrowsException() {
        assertThatThrownBy(() -> CostCalculationUtil.calculateTotalCost(0, new BigDecimal("5000.00")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> CostCalculationUtil.calculateTotalCost(3, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

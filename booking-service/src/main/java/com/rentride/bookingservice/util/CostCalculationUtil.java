package com.rentride.bookingservice.util;

import com.rentride.bookingservice.exception.InvalidBookingDatesException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Utility for rental duration and precise monetary cost calculations.
 */
public final class CostCalculationUtil {

    private CostCalculationUtil() {
        // Prevent instantiation
    }

    /**
     * Calculates the total rental duration in days according to RentRide platform business rules:
     * <ul>
     *   <li>Standard interval: {@code ChronoUnit.DAYS.between(startDate, endDate)}</li>
     *   <li>Same-day rental: if {@code startDate.equals(endDate)}, counts as 1 full day rental.</li>
     * </ul>
     *
     * @param startDate rental start date (inclusive)
     * @param endDate rental end date (inclusive)
     * @return total rental days (minimum 1)
     * @throws InvalidBookingDatesException if end date is before start date
     * @throws IllegalArgumentException if either date is null
     */
    public static int calculateRentalDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date must not be null");
        }
        if (endDate.isBefore(startDate)) {
            throw new InvalidBookingDatesException(
                    String.format("End date (%s) cannot be before start date (%s)", endDate, startDate));
        }

        long days = ChronoUnit.DAYS.between(startDate, endDate);
        return startDate.equals(endDate) ? 1 : (int) days;
    }

    /**
     * Calculates the total cost for a reservation using precise BigDecimal arithmetic.
     *
     * @param totalDays number of rental days (must be >= 1)
     * @param dailyRentalRate daily rate charged for the vehicle
     * @return total rental cost scaled to 2 decimal places with HALF_UP rounding
     * @throws IllegalArgumentException if totalDays <= 0 or dailyRentalRate is null
     */
    public static BigDecimal calculateTotalCost(int totalDays, BigDecimal dailyRentalRate) {
        if (dailyRentalRate == null) {
            throw new IllegalArgumentException("Daily rental rate must not be null");
        }
        if (totalDays <= 0) {
            throw new IllegalArgumentException("Total rental days must be greater than zero");
        }

        return dailyRentalRate.multiply(BigDecimal.valueOf(totalDays))
                .setScale(2, RoundingMode.HALF_UP);
    }
}


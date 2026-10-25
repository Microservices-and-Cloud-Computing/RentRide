package com.rentride.bookingservice.repository;

import com.rentride.bookingservice.entity.Booking;
import com.rentride.bookingservice.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Booking} reservation entities.
 *
 * <p>Provides lookup methods by business reference, customer history, provider fleet reservations,
 * vehicle status tracking, and interval conflict detection for overlapping dates.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Finds a booking by its unique business reservation reference.
     *
     * @param bookingReference alphanumeric reservation code
     * @return Optional containing matching Booking if found
     */
    Optional<Booking> findByBookingReference(String bookingReference);

    /**
     * Retrieves all bookings for a customer ordered from newest to oldest.
     *
     * @param customerId unique ID of the customer
     * @return list of bookings placed by the customer
     */
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    /**
     * Retrieves all bookings for a provider's fleet ordered from newest to oldest.
     *
     * @param providerId unique ID of the rental provider
     * @return list of bookings associated with the provider
     */
    List<Booking> findByProviderIdOrderByCreatedAtDesc(Long providerId);

    /**
     * Retrieves bookings for a specific vehicle filtered by matching lifecycle statuses.
     *
     * @param vehicleId unique ID of the vehicle
     * @param statuses list of booking lifecycle statuses to filter by
     * @return list of matching vehicle bookings
     */
    List<Booking> findByVehicleIdAndStatusIn(Long vehicleId, List<BookingStatus> statuses);

    /**
     * Checks if any active reservation exists for the specified vehicle that overlaps
     * with the requested date range.
     *
     * <p>Two closed intervals [start1, end1] and [start2, end2] overlap if and only if:
     * {@code start1 <= end2 AND end1 >= start2}.
     *
     * @param vehicleId unique ID of the vehicle
     * @param activeStatuses list of active booking statuses (e.g. PENDING_PAYMENT, CONFIRMED, ACTIVE_RENTAL)
     * @param endDate requested reservation end date (compared against existing start_date <= endDate)
     * @param startDate requested reservation start date (compared against existing end_date >= startDate)
     * @return {@code true} if an overlapping active booking exists, {@code false} otherwise
     */
    boolean existsByVehicleIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long vehicleId,
            List<BookingStatus> activeStatuses,
            LocalDate endDate,
            LocalDate startDate
    );
}


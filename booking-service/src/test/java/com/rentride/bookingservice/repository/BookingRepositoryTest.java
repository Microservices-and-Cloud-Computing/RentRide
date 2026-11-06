package com.rentride.bookingservice.repository;

import com.rentride.bookingservice.entity.Booking;
import com.rentride.bookingservice.entity.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Data JPA Slice tests for {@link BookingRepository}.
 *
 * <p>Validates entity mapping, surrogate key generation, unique reference constraints,
 * audit timestamps, and custom query methods including the date interval overlap detection.
 */
@DataJpaTest
@ActiveProfiles("test")
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(
            BookingStatus.PENDING_PAYMENT,
            BookingStatus.CONFIRMED,
            BookingStatus.ACTIVE_RENTAL
    );

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        entityManager.clear();
    }

    private Booking createSampleBooking(String reference, Long customerId, Long providerId, Long vehicleId,
                                         LocalDate startDate, LocalDate endDate, BookingStatus status) {
        return Booking.builder()
                .bookingReference(reference)
                .customerId(customerId)
                .providerId(providerId)
                .vehicleId(vehicleId)
                .startDate(startDate)
                .endDate(endDate)
                .totalDays((int) (endDate.toEpochDay() - startDate.toEpochDay() + 1))
                .dailyRate(new BigDecimal("5000.00"))
                .totalCost(new BigDecimal("25000.00"))
                .status(status)
                .pickupNotes("Valid license verified at pickup")
                .returnNotes("Returned with full fuel tank")
                .build();
    }

    @Test
    @DisplayName("a) testSaveBooking_Success: Persist booking with status PENDING_PAYMENT, verify ID and timestamps")
    void testSaveBooking_Success() {
        Booking booking = createSampleBooking("RR-BK-20261025-ABC123", 101L, 201L, 301L,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), BookingStatus.PENDING_PAYMENT);

        Booking saved = bookingRepository.saveAndFlush(booking);

        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getBookingReference()).isEqualTo("RR-BK-20261025-ABC123");
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(saved.getCustomerId()).isEqualTo(101L);
        assertThat(saved.getProviderId()).isEqualTo(201L);
        assertThat(saved.getVehicleId()).isEqualTo(301L);
        assertThat(saved.getTotalDays()).isEqualTo(5);
        assertThat(saved.getDailyRate()).isEqualByComparingTo("5000.00");
        assertThat(saved.getTotalCost()).isEqualByComparingTo("25000.00");
    }

    @Test
    @DisplayName("b) testFindByBookingReference_Success: Save booking RR-BK-TEST-001 and verify all fields match")
    void testFindByBookingReference_Success() {
        Booking booking = Booking.builder()
                .bookingReference("RR-BK-TEST-001")
                .customerId(10L)
                .providerId(20L)
                .vehicleId(30L)
                .startDate(LocalDate.of(2026, 11, 5))
                .endDate(LocalDate.of(2026, 11, 8))
                .totalDays(4)
                .dailyRate(new BigDecimal("6000.00"))
                .totalCost(new BigDecimal("24000.00"))
                .status(BookingStatus.CONFIRMED)
                .pickupNotes("Test pickup notes")
                .returnNotes("Test return notes")
                .build();

        bookingRepository.saveAndFlush(booking);

        Optional<Booking> foundOpt = bookingRepository.findByBookingReference("RR-BK-TEST-001");

        assertThat(foundOpt).isPresent();
        Booking found = foundOpt.get();
        assertThat(found.getBookingReference()).isEqualTo("RR-BK-TEST-001");
        assertThat(found.getCustomerId()).isEqualTo(10L);
        assertThat(found.getProviderId()).isEqualTo(20L);
        assertThat(found.getVehicleId()).isEqualTo(30L);
        assertThat(found.getStartDate()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(found.getEndDate()).isEqualTo(LocalDate.of(2026, 11, 8));
        assertThat(found.getTotalDays()).isEqualTo(4);
        assertThat(found.getDailyRate()).isEqualByComparingTo("6000.00");
        assertThat(found.getTotalCost()).isEqualByComparingTo("24000.00");
        assertThat(found.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(found.getPickupNotes()).isEqualTo("Test pickup notes");
        assertThat(found.getReturnNotes()).isEqualTo("Test return notes");
    }

    @Test
    @DisplayName("c) testDuplicateBookingReference_ThrowsException: Inserting duplicate reference throws DataIntegrityViolationException")
    void testDuplicateBookingReference_ThrowsException() {
        Booking first = createSampleBooking("RR-BK-UNIQUE-001", 1L, 10L, 20L,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), BookingStatus.CONFIRMED);
        bookingRepository.saveAndFlush(first);

        Booking duplicate = createSampleBooking("RR-BK-UNIQUE-001", 2L, 11L, 21L,
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 8), BookingStatus.PENDING_PAYMENT);

        assertThrows(DataIntegrityViolationException.class, () -> bookingRepository.saveAndFlush(duplicate));
    }

    @Test
    @DisplayName("d) testFindByCustomerId_ReturnsSortedList: Save bookings for customer ID 10 and verify newest first")
    void testFindByCustomerId_ReturnsSortedList() throws InterruptedException {
        Booking booking1 = createSampleBooking("RR-BK-20261025-CUST01", 10L, 201L, 301L,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), BookingStatus.CONFIRMED);
        bookingRepository.saveAndFlush(booking1);

        Thread.sleep(20);

        Booking booking2 = createSampleBooking("RR-BK-20261025-CUST02", 10L, 202L, 302L,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15), BookingStatus.COMPLETED);
        bookingRepository.saveAndFlush(booking2);

        List<Booking> results = bookingRepository.findByCustomerIdOrderByCreatedAtDesc(10L);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getBookingReference()).isEqualTo("RR-BK-20261025-CUST02");
        assertThat(results.get(1).getBookingReference()).isEqualTo("RR-BK-20261025-CUST01");
        assertThat(results.get(0).getCreatedAt()).isAfterOrEqualTo(results.get(1).getCreatedAt());
    }

    @Test
    @DisplayName("e) testOverlapDetectionQuery: Vehicle 100 on 2026-11-01 to 2026-11-05, check overlapping (11-03 to 11-07) and non-overlapping (11-10 to 11-15)")
    void testOverlapDetectionQuery() {
        Long vehicleId = 100L;
        // Persist booking for vehicle 100 spanning from 2026-11-01 to 2026-11-05 with status CONFIRMED
        bookingRepository.saveAndFlush(createSampleBooking("RR-BK-CONFIRMED-100", 10L, 20L, vehicleId,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), BookingStatus.CONFIRMED));

        // Overlapping range: 2026-11-03 to 2026-11-07
        // Method signature: (vehicleId, activeStatuses, requestedEndDate, requestedStartDate)
        boolean isOverlapping = bookingRepository
                .existsByVehicleIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        vehicleId,
                        ACTIVE_STATUSES,
                        LocalDate.of(2026, 11, 7), // requestedEndDate
                        LocalDate.of(2026, 11, 3)  // requestedStartDate
                );
        assertThat(isOverlapping)
                .as("Requested range 2026-11-03 to 2026-11-07 should overlap with existing 2026-11-01 to 2026-11-05")
                .isTrue();

        // Non-overlapping range: 2026-11-10 to 2026-11-15
        boolean isNonOverlapping = bookingRepository
                .existsByVehicleIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        vehicleId,
                        ACTIVE_STATUSES,
                        LocalDate.of(2026, 11, 15), // requestedEndDate
                        LocalDate.of(2026, 11, 10)  // requestedStartDate
                );
        assertThat(isNonOverlapping)
                .as("Requested range 2026-11-10 to 2026-11-15 should NOT overlap with existing 2026-11-01 to 2026-11-05")
                .isFalse();
    }

    @Test
    @DisplayName("Should return empty Optional when bookingReference does not exist")
    void shouldReturnEmptyWhenReferenceNotFound() {
        Optional<Booking> found = bookingRepository.findByBookingReference("NON-EXISTENT-REF");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find bookings by providerId ordered by createdAt descending")
    void shouldFindByProviderIdOrderByCreatedAtDesc() throws InterruptedException {
        Booking booking1 = createSampleBooking("RR-BK-20261025-PROV01", 101L, 777L, 301L,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), BookingStatus.CONFIRMED);
        bookingRepository.saveAndFlush(booking1);

        Thread.sleep(20);

        Booking booking2 = createSampleBooking("RR-BK-20261025-PROV02", 102L, 777L, 302L,
                LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 6), BookingStatus.ACTIVE_RENTAL);
        bookingRepository.saveAndFlush(booking2);

        List<Booking> results = bookingRepository.findByProviderIdOrderByCreatedAtDesc(777L);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getBookingReference()).isEqualTo("RR-BK-20261025-PROV02");
        assertThat(results.get(1).getBookingReference()).isEqualTo("RR-BK-20261025-PROV01");
    }

    @Test
    @DisplayName("Should find bookings by vehicleId and matching statuses")
    void shouldFindByVehicleIdAndStatusIn() {
        Long vehicleId = 999L;
        bookingRepository.saveAndFlush(createSampleBooking("RR-BK-V1", 1L, 10L, vehicleId,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), BookingStatus.PENDING_PAYMENT));
        bookingRepository.saveAndFlush(createSampleBooking("RR-BK-V2", 2L, 10L, vehicleId,
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 8), BookingStatus.CONFIRMED));
        bookingRepository.saveAndFlush(createSampleBooking("RR-BK-V3", 3L, 10L, vehicleId,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12), BookingStatus.CANCELLED));

        List<Booking> active = bookingRepository.findByVehicleIdAndStatusIn(
                vehicleId, List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED));

        assertThat(active).hasSize(2);
        assertThat(active).extracting(Booking::getStatus)
                .containsExactlyInAnyOrder(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Should not detect overlap if existing booking status is CANCELLED or COMPLETED")
    void shouldNotDetectOverlapForInactiveStatuses() {
        Long vehicleId = 406L;
        bookingRepository.saveAndFlush(createSampleBooking("RR-BK-CANCELLED", 1L, 10L, vehicleId,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15), BookingStatus.CANCELLED));

        boolean hasConflict = bookingRepository
                .existsByVehicleIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        vehicleId, ACTIVE_STATUSES, LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 10));

        assertThat(hasConflict).isFalse();
    }
}

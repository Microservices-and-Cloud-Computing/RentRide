package com.rentride.bookingservice.service;

import com.rentride.bookingservice.dto.BookingCreateRequest;
import com.rentride.bookingservice.dto.BookingResponse;
import com.rentride.bookingservice.dto.external.VehicleResponseDto;
import com.rentride.bookingservice.entity.Booking;
import com.rentride.bookingservice.entity.BookingStatus;
import com.rentride.bookingservice.exception.BookingConflictException;
import com.rentride.bookingservice.exception.BookingNotFoundException;
import com.rentride.bookingservice.exception.InvalidBookingDatesException;
import com.rentride.bookingservice.exception.UnauthorizedBookingAccessException;
import com.rentride.bookingservice.exception.VehicleUnavailableException;
import com.rentride.bookingservice.repository.BookingRepository;
import com.rentride.bookingservice.util.BookingReferenceGenerator;
import com.rentride.bookingservice.util.CostCalculationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Implementation of {@link BookingService} handling reservation creation, cost calculations,
 * OpenFeign inter-service vehicle status locking, and conflict validation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    public static final int MAX_RENTAL_DAYS = 30;
    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES = List.of(
            BookingStatus.PENDING_PAYMENT,
            BookingStatus.CONFIRMED,
            BookingStatus.ACTIVE_RENTAL
    );

    private final BookingRepository bookingRepository;
    private final VehicleIntegrationService vehicleIntegrationService;
    private final BookingReferenceGenerator bookingReferenceGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingResponse createBooking(Long customerId, BookingCreateRequest request) {
        log.info("Initiating booking creation: customerId={}, vehicleId={}, dates=[{} to {}]",
                customerId, request.getVehicleId(), request.getStartDate(), request.getEndDate());

        // 1. Validate dates: endDate >= startDate, maximum rental window limit (30 days), not in past
        validateBookingDates(request.getStartDate(), request.getEndDate());

        Long vehicleId = request.getVehicleId();

        // 2. Fetch vehicle details via OpenFeign and verify availability
        VehicleResponseDto vehicle = vehicleIntegrationService.getVehicleById(vehicleId);
        if (vehicle == null || vehicle.getStatus() == null || !"AVAILABLE".equalsIgnoreCase(vehicle.getStatus())) {
            String currentStatus = (vehicle != null && vehicle.getStatus() != null) ? vehicle.getStatus() : "UNKNOWN";
            log.warn("Vehicle ID {} is not available for booking (status: {})", vehicleId, currentStatus);
            throw new VehicleUnavailableException(vehicleId, currentStatus);
        }

        // 3. Query BookingRepository to ensure no overlapping active bookings exist for this vehicle
        boolean hasOverlap = bookingRepository.existsByVehicleIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                vehicleId,
                ACTIVE_BOOKING_STATUSES,
                request.getEndDate(),
                request.getStartDate()
        );

        if (hasOverlap) {
            log.warn("Booking conflict detected for vehicle ID {} between {} and {}",
                    vehicleId, request.getStartDate(), request.getEndDate());
            throw new BookingConflictException(vehicleId, request.getStartDate(), request.getEndDate());
        }

        // 4. Calculate total rental days and total cost
        int totalDays = CostCalculationUtil.calculateRentalDays(request.getStartDate(), request.getEndDate());
        BigDecimal totalCost = CostCalculationUtil.calculateTotalCost(totalDays, vehicle.getDailyRentalRate());
        log.debug("Calculated reservation duration: {} days, totalCost: {}", totalDays, totalCost);

        // 5. Generate unique booking reference
        String bookingReference = bookingReferenceGenerator.generate();

        // 6. Lock vehicle via OpenFeign to prevent concurrent race collisions
        log.info("Locking vehicle ID {} with status RENTED for reference {}", vehicleId, bookingReference);
        vehicleIntegrationService.updateVehicleStatus(vehicleId, "RENTED", "Locked by Booking: " + bookingReference);

        // 7. Persist new Booking entity with compensating rollback if local DB fails
        Booking savedBooking;
        try {
            Booking booking = Booking.builder()
                    .bookingReference(bookingReference)
                    .customerId(customerId)
                    .providerId(vehicle.getProviderId())
                    .vehicleId(vehicleId)
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .totalDays(totalDays)
                    .dailyRate(vehicle.getDailyRentalRate())
                    .totalCost(totalCost)
                    .status(BookingStatus.PENDING_PAYMENT)
                    .pickupNotes(request.getPickupNotes())
                    .build();

            savedBooking = bookingRepository.save(booking);
            log.info("Booking successfully created with ID: {}, reference: {}", savedBooking.getId(), savedBooking.getBookingReference());
        } catch (Exception ex) {
            log.error("Failed to persist booking for vehicle ID {}. Executing compensating rollback on vehicle-service", vehicleId, ex);
            try {
                vehicleIntegrationService.updateVehicleStatus(vehicleId, "AVAILABLE", "Compensating rollback: booking creation failed");
            } catch (Exception rollbackEx) {
                log.error("Compensating rollback failed to unlock vehicle ID {}", vehicleId, rollbackEx);
            }
            throw ex;
        }

        // 8. Map and return response
        return mapToResponse(savedBooking, vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id, Long requesterId, String userRole) {
        log.info("Fetching booking details: id={}, requesterId={}, role={}", id, requesterId, userRole);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        // Ownership and authorization validation
        if (requesterId != null) {
            boolean isAdmin = userRole != null && (userRole.contains("ADMIN") || userRole.contains("ROLE_ADMIN"));
            boolean isOwner = booking.getCustomerId().equals(requesterId) || booking.getProviderId().equals(requesterId);
            if (!isAdmin && !isOwner) {
                log.warn("Access denied for requesterId={} to bookingId={}", requesterId, id);
                throw new UnauthorizedBookingAccessException("You do not have permission to view reservation ID: " + id);
            }
        }

        // Attempt to fetch current vehicle specifications via OpenFeign
        VehicleResponseDto vehicle = null;
        try {
            vehicle = vehicleIntegrationService.getVehicleById(booking.getVehicleId());
        } catch (Exception e) {
            log.warn("Could not retrieve upstream vehicle specs for vehicleId={}: {}", booking.getVehicleId(), e.getMessage());
        }

        return mapToResponse(booking, vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByCustomerId(Long customerId) {
        log.info("Fetching booking history for customerId={}", customerId);
        List<Booking> bookings = bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);

        return bookings.stream()
                .map(booking -> {
                    VehicleResponseDto vehicle = null;
                    try {
                        vehicle = vehicleIntegrationService.getVehicleById(booking.getVehicleId());
                    } catch (Exception e) {
                        log.debug("Upstream vehicle lookup omitted for booking ID: {}", booking.getId());
                    }
                    return mapToResponse(booking, vehicle);
                })
                .toList();
    }

    private void validateBookingDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidBookingDatesException("Start date and end date are required");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new InvalidBookingDatesException("Reservation start date cannot be in the past: " + startDate);
        }
        if (endDate.isBefore(startDate)) {
            throw new InvalidBookingDatesException(
                    String.format("Reservation end date (%s) cannot be before start date (%s)", endDate, startDate));
        }

        int days = CostCalculationUtil.calculateRentalDays(startDate, endDate);
        if (days > MAX_RENTAL_DAYS) {
            throw new InvalidBookingDatesException(
                    String.format("Rental duration (%d days) exceeds maximum allowed limit of %d days", days, MAX_RENTAL_DAYS));
        }
    }

    private BookingResponse mapToResponse(Booking booking, VehicleResponseDto vehicle) {
        BookingResponse.VehicleDetails vehicleDetails = null;
        if (vehicle != null) {
            vehicleDetails = BookingResponse.VehicleDetails.builder()
                    .make(vehicle.getMake())
                    .model(vehicle.getModel())
                    .licensePlateNumber(vehicle.getLicensePlateNumber())
                    .categoryName(vehicle.getCategoryName())
                    .location(vehicle.getLocation())
                    .build();
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .customerId(booking.getCustomerId())
                .providerId(booking.getProviderId())
                .vehicleId(booking.getVehicleId())
                .vehicleDetails(vehicleDetails)
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .totalDays(booking.getTotalDays())
                .dailyRate(booking.getDailyRate())
                .totalCost(booking.getTotalCost())
                .status(booking.getStatus())
                .pickupNotes(booking.getPickupNotes())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}


package com.rentride.bookingservice.service;

import com.rentride.bookingservice.dto.BookingCreateRequest;
import com.rentride.bookingservice.dto.BookingLifecycleResponse;
import com.rentride.bookingservice.dto.BookingPaymentCallbackRequest;
import com.rentride.bookingservice.dto.BookingResponse;
import com.rentride.bookingservice.dto.ReturnCondition;
import com.rentride.bookingservice.dto.VehiclePickupRequest;
import com.rentride.bookingservice.dto.VehicleReturnRequest;
import com.rentride.bookingservice.dto.external.VehicleResponseDto;
import com.rentride.bookingservice.entity.Booking;
import com.rentride.bookingservice.entity.BookingStatus;
import com.rentride.bookingservice.exception.BookingConflictException;
import com.rentride.bookingservice.exception.BookingNotFoundException;
import com.rentride.bookingservice.exception.InvalidBookingDatesException;
import com.rentride.bookingservice.exception.InvalidBookingStateException;
import com.rentride.bookingservice.exception.UnauthorizedBookingAccessException;
import com.rentride.bookingservice.exception.UnauthorizedProviderException;
import com.rentride.bookingservice.exception.VehicleNotAvailableException;
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
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of {@link BookingService} handling reservation creation, cost calculations,
 * OpenFeign inter-service vehicle status locking, and lifecycle status transitions (pickup & return).
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
                    .additionalCharges(BigDecimal.ZERO)
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingResponse confirmBooking(Long bookingId) {
        log.info("Confirming booking reservation: bookingId={}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            log.warn("Invalid confirm state transition: bookingId={}, currentStatus={}", bookingId, booking.getStatus());
            throw new InvalidBookingStateException(String.format(
                    "Booking %s cannot be confirmed: current status is %s (must be PENDING_PAYMENT)",
                    booking.getBookingReference(), booking.getStatus()));
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} successfully transitioned to CONFIRMED", saved.getBookingReference());

        VehicleResponseDto vehicle = null;
        try {
            vehicle = vehicleIntegrationService.getVehicleById(saved.getVehicleId());
        } catch (Exception e) {
            log.debug("Vehicle lookup omitted during booking confirmation");
        }

        return mapToResponse(saved, vehicle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingResponse updatePaymentStatus(Long id, BookingPaymentCallbackRequest request) {
        log.info("Processing payment callback: bookingId={}, txnRef={}, status={}, amount={}",
                id, request.getTransactionReference(), request.getPaymentStatus(), request.getPaidAmount());

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        if ("SUCCESS".equalsIgnoreCase(request.getPaymentStatus())) {
            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
                log.warn("Invalid payment status transition: bookingId={}, currentStatus={}", id, booking.getStatus());
                throw new InvalidBookingStateException(String.format(
                        "Booking %s cannot be confirmed: current status is %s (must be PENDING_PAYMENT)",
                        booking.getBookingReference(), booking.getStatus()));
            }

            booking.setStatus(BookingStatus.CONFIRMED);
            Booking saved = bookingRepository.save(booking);
            log.info("Booking {} transitioned to CONFIRMED via payment callback {}",
                    saved.getBookingReference(), request.getTransactionReference());

            VehicleResponseDto vehicle = null;
            try {
                vehicle = vehicleIntegrationService.getVehicleById(saved.getVehicleId());
            } catch (Exception e) {
                log.debug("Vehicle lookup omitted during payment callback confirmation");
            }
            return mapToResponse(saved, vehicle);
        } else {
            log.warn("Payment callback indicates payment failure for booking {}: txnRef={}",
                    booking.getBookingReference(), request.getTransactionReference());
            VehicleResponseDto vehicle = null;
            try {
                vehicle = vehicleIntegrationService.getVehicleById(booking.getVehicleId());
            } catch (Exception e) {
                log.debug("Vehicle lookup omitted during payment callback failure");
            }
            return mapToResponse(booking, vehicle);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingLifecycleResponse recordVehiclePickup(Long bookingId, Long providerId, VehiclePickupRequest request) {
        log.info("Recording vehicle pickup: bookingId={}, providerId={}, odometer={}, fuel={}%",
                bookingId, providerId, request.getOdometerReading(), request.getFuelLevelPercent());

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // 1. Verify provider authorization
        if (!booking.getProviderId().equals(providerId)) {
            log.warn("Unauthorized pickup attempt: bookingId={}, expectedProvider={}, actualProvider={}",
                    bookingId, booking.getProviderId(), providerId);
            throw new UnauthorizedProviderException("You do not have permission to manage handovers for booking ID: " + bookingId);
        }

        // 2. Verify state is CONFIRMED
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            log.warn("Invalid pickup state transition: bookingId={}, currentStatus={}", bookingId, booking.getStatus());
            throw new InvalidBookingStateException(String.format(
                    "Vehicle pickup cannot be recorded for booking %s: current status is %s (must be CONFIRMED)",
                    booking.getBookingReference(), booking.getStatus()));
        }

        // 3. Update booking status and initial pickup metrics
        booking.setStatus(BookingStatus.ACTIVE_RENTAL);
        booking.setActualPickupTime(LocalDateTime.now());
        booking.setStartOdometer(request.getOdometerReading());
        booking.setStartFuelPercent(request.getFuelLevelPercent());

        if (request.getHandoverNotes() != null && !request.getHandoverNotes().isBlank()) {
            String current = booking.getPickupNotes() != null ? booking.getPickupNotes() + " | " : "";
            booking.setPickupNotes(current + request.getHandoverNotes());
        }

        // 4. Sync with upstream Vehicle-Service
        try {
            vehicleIntegrationService.updateVehicleStatus(
                    booking.getVehicleId(),
                    "RENTED",
                    "Active Rental Handover: " + booking.getBookingReference()
            );
        } catch (VehicleNotAvailableException ex) {
            // Vehicle was already transitioned to RENTED during initial reservation lock
            log.info("Vehicle ID {} is already marked as RENTED in vehicle-service (idempotent handover)",
                    booking.getVehicleId());
        }

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} successfully transitioned to ACTIVE_RENTAL", saved.getBookingReference());

        return mapToLifecycleResponse(saved, "RENTED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookingLifecycleResponse recordVehicleReturn(Long bookingId, Long providerId, VehicleReturnRequest request) {
        log.info("Recording vehicle return: bookingId={}, providerId={}, condition={}, odometer={}, additionalCharges={}",
                bookingId, providerId, request.getVehicleCondition(), request.getOdometerReading(), request.getAdditionalCharges());

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // 1. Verify provider authorization
        if (!booking.getProviderId().equals(providerId)) {
            log.warn("Unauthorized return attempt: bookingId={}, expectedProvider={}, actualProvider={}",
                    bookingId, booking.getProviderId(), providerId);
            throw new UnauthorizedProviderException("You do not have permission to manage returns for booking ID: " + bookingId);
        }

        // 2. Verify state is ACTIVE_RENTAL
        if (booking.getStatus() != BookingStatus.ACTIVE_RENTAL) {
            log.warn("Invalid return state transition: bookingId={}, currentStatus={}", bookingId, booking.getStatus());
            throw new InvalidBookingStateException(String.format(
                    "Vehicle return cannot be processed for booking %s: current status is %s (must be ACTIVE_RENTAL)",
                    booking.getBookingReference(), booking.getStatus()));
        }

        // 3. Update booking status and return metrics
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setActualReturnDate(LocalDateTime.now());
        booking.setEndOdometer(request.getOdometerReading());
        booking.setEndFuelPercent(request.getFuelLevelPercent());

        BigDecimal extraCharges = request.getAdditionalCharges() != null ? request.getAdditionalCharges() : BigDecimal.ZERO;
        booking.setAdditionalCharges(extraCharges);

        if (request.getReturnNotes() != null && !request.getReturnNotes().isBlank()) {
            String current = booking.getReturnNotes() != null ? booking.getReturnNotes() + " | " : "";
            booking.setReturnNotes(current + request.getReturnNotes());
        }

        // 4. Determine downstream vehicle status
        String downstreamStatus;
        String reason;
        if (request.getVehicleCondition() == ReturnCondition.GOOD) {
            downstreamStatus = "AVAILABLE";
            reason = "Return completed in good condition: " + booking.getBookingReference();
        } else {
            downstreamStatus = "MAINTENANCE";
            reason = "Returned with reported damages / maintenance required: " + booking.getBookingReference();
        }

        vehicleIntegrationService.updateVehicleStatus(booking.getVehicleId(), downstreamStatus, reason);

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} successfully transitioned to COMPLETED. Downstream vehicle status: {}",
                saved.getBookingReference(), downstreamStatus);

        return mapToLifecycleResponse(saved, downstreamStatus);
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

    private BookingLifecycleResponse mapToLifecycleResponse(Booking booking, String vehicleStatusSummary) {
        BigDecimal additional = booking.getAdditionalCharges() != null ? booking.getAdditionalCharges() : BigDecimal.ZERO;
        BigDecimal finalAmountPaid = booking.getTotalCost().add(additional);

        return BookingLifecycleResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .customerId(booking.getCustomerId())
                .providerId(booking.getProviderId())
                .vehicleId(booking.getVehicleId())
                .status(booking.getStatus())
                .actualPickupTime(booking.getActualPickupTime())
                .actualReturnDate(booking.getActualReturnDate())
                .vehicleStatusSummary(vehicleStatusSummary)
                .startOdometer(booking.getStartOdometer())
                .endOdometer(booking.getEndOdometer())
                .startFuelPercent(booking.getStartFuelPercent())
                .endFuelPercent(booking.getEndFuelPercent())
                .totalCost(booking.getTotalCost())
                .additionalCharges(additional)
                .finalAmountPaid(finalAmountPaid)
                .pickupNotes(booking.getPickupNotes())
                .returnNotes(booking.getReturnNotes())
                .build();
    }
}

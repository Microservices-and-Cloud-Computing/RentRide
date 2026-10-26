package com.rentride.bookingservice.controller;

import com.rentride.bookingservice.dto.BookingCreateRequest;
import com.rentride.bookingservice.dto.BookingLifecycleResponse;
import com.rentride.bookingservice.dto.BookingResponse;
import com.rentride.bookingservice.dto.VehiclePickupRequest;
import com.rentride.bookingservice.dto.VehicleReturnRequest;
import com.rentride.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for customer vehicle reservation operations and provider handover/return lifecycles.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * Creates a new vehicle reservation for the authenticated customer.
     *
     * @param customerId ID of the authenticated user injected by API Gateway
     * @param request reservation parameters
     * @return 201 Created with BookingResponse
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<BookingResponse> createBooking(
            @RequestHeader("X-User-Id") Long customerId,
            @Valid @RequestBody BookingCreateRequest request) {
        log.info("REST POST /api/v1/bookings: customerId={}, vehicleId={}", customerId, request.getVehicleId());
        BookingResponse response = bookingService.createBooking(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves details of a specific reservation by ID.
     *
     * @param id primary booking ID
     * @param requesterId ID of the requesting user from header
     * @param userRole role of the requesting user from header
     * @return 200 OK with BookingResponse
     */
    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        log.info("REST GET /api/v1/bookings/{}: requesterId={}, role={}", id, requesterId, userRole);
        BookingResponse response = bookingService.getBookingById(id, requesterId, userRole);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all reservations belonging to the authenticated customer.
     *
     * @param customerId ID of the authenticated customer injected by API Gateway
     * @return 200 OK with list of BookingResponse
     */
    @GetMapping("/customer/{customerId:[0-9]+}")
    public ResponseEntity<List<BookingResponse>> getBookingsByCustomerId(
            @PathVariable("customerId") Long customerId) {
        log.info("REST GET /api/v1/bookings/customer/{}: customerId={}", customerId, customerId);
        List<BookingResponse> responses = bookingService.getBookingsByCustomerId(customerId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/customer/my-bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @RequestHeader("X-User-Id") Long customerId) {
        log.info("REST GET /api/v1/bookings/customer/my-bookings: customerId={}", customerId);
        List<BookingResponse> responses = bookingService.getBookingsByCustomerId(customerId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Confirms a reservation upon payment verification, advancing status from PENDING_PAYMENT to CONFIRMED.
     *
     * @param id primary booking ID
     * @return 200 OK with BookingResponse
     */
    @PostMapping("/{id:[0-9]+}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable("id") Long id) {
        log.info("REST POST /api/v1/bookings/{}/confirm: Advancing status to CONFIRMED", id);
        BookingResponse response = bookingService.confirmBooking(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Records customer vehicle pickup / handover, advancing status from CONFIRMED to ACTIVE_RENTAL.
     *
     * @param id primary booking ID
     * @param providerId ID of the vehicle provider performing the handover
     * @param request pickup statistics (odometer, fuel level, notes)
     * @return 200 OK with BookingLifecycleResponse
     */
    @PostMapping("/{id:[0-9]+}/pickup")
    public ResponseEntity<BookingLifecycleResponse> recordVehiclePickup(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long providerId,
            @Valid @RequestBody VehiclePickupRequest request) {
        log.info("REST POST /api/v1/bookings/{}/pickup: providerId={}", id, providerId);
        BookingLifecycleResponse response = bookingService.recordVehiclePickup(id, providerId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Records customer vehicle return inspection, advancing status from ACTIVE_RENTAL to COMPLETED
     * and synchronizing upstream vehicle status to AVAILABLE or MAINTENANCE.
     *
     * @param id primary booking ID
     * @param providerId ID of the vehicle provider receiving the return
     * @param request return statistics, condition, notes, and any additional charges
     * @return 200 OK with BookingLifecycleResponse
     */
    @PostMapping("/{id:[0-9]+}/return")
    public ResponseEntity<BookingLifecycleResponse> recordVehicleReturn(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long providerId,
            @Valid @RequestBody VehicleReturnRequest request) {
        log.info("REST POST /api/v1/bookings/{}/return: providerId={}", id, providerId);
        BookingLifecycleResponse response = bookingService.recordVehicleReturn(id, providerId, request);
        return ResponseEntity.ok(response);
    }
}

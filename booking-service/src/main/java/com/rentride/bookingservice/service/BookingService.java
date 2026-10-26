package com.rentride.bookingservice.service;

import com.rentride.bookingservice.dto.BookingCreateRequest;
import com.rentride.bookingservice.dto.BookingLifecycleResponse;
import com.rentride.bookingservice.dto.BookingResponse;
import com.rentride.bookingservice.dto.VehiclePickupRequest;
import com.rentride.bookingservice.dto.VehicleReturnRequest;

import java.util.List;

/**
 * Service interface managing vehicle reservation lifecycles.
 */
public interface BookingService {

    /**
     * Creates a new vehicle reservation in PENDING_PAYMENT status.
     *
     * @param customerId ID of the authenticated reserving customer
     * @param request reservation details including vehicle, dates, and optional notes
     * @return BookingResponse with calculated costs and reservation details
     */
    BookingResponse createBooking(Long customerId, BookingCreateRequest request);

    /**
     * Retrieves booking reservation details by ID.
     *
     * @param id booking primary key ID
     * @param requesterId ID of the user requesting details (for ownership validation)
     * @param userRole role of the requester (ADMIN bypasses ownership check)
     * @return BookingResponse with reservation specifications
     */
    BookingResponse getBookingById(Long id, Long requesterId, String userRole);

    /**
     * Retrieves all reservations belonging to a customer, ordered newest first.
     *
     * @param customerId unique ID of the customer
     * @return list of BookingResponse DTOs
     */
    List<BookingResponse> getBookingsByCustomerId(Long customerId);

    /**
     * Confirms a booking reservation upon payment verification, advancing status from PENDING_PAYMENT to CONFIRMED.
     *
     * @param bookingId booking ID
     * @return BookingResponse with CONFIRMED status
     */
    BookingResponse confirmBooking(Long bookingId);

    /**
     * Records customer vehicle pickup handover, moving reservation from CONFIRMED to ACTIVE_RENTAL.
     *
     * @param bookingId booking ID
     * @param providerId ID of the vehicle owner / provider performing the handover
     * @param request handover statistics (odometer, fuel, notes)
     * @return BookingLifecycleResponse with updated status and pickup metrics
     */
    BookingLifecycleResponse recordVehiclePickup(Long bookingId, Long providerId, VehiclePickupRequest request);

    /**
     * Records customer vehicle return inspection, moving reservation from ACTIVE_RENTAL to COMPLETED
     * and synchronizing upstream vehicle status to AVAILABLE or MAINTENANCE.
     *
     * @param bookingId booking ID
     * @param providerId ID of the vehicle owner / provider receiving the return
     * @param request return statistics, condition, notes, and any additional charges
     * @return BookingLifecycleResponse with finalized rental costs and completion status
     */
    BookingLifecycleResponse recordVehicleReturn(Long bookingId, Long providerId, VehicleReturnRequest request);
}

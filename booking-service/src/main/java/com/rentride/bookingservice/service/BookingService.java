package com.rentride.bookingservice.service;

import com.rentride.bookingservice.dto.BookingCreateRequest;
import com.rentride.bookingservice.dto.BookingResponse;

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
}


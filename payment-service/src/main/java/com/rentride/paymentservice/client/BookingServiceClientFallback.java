package com.rentride.paymentservice.client;

import com.rentride.paymentservice.dto.external.BookingPaymentCallbackRequest;
import com.rentride.paymentservice.dto.external.BookingSummaryResponse;
import com.rentride.paymentservice.exception.BookingServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for BookingServiceClient when the circuit breaker opens or booking-service times out.
 */
@Slf4j
@Component
public class BookingServiceClientFallback implements BookingServiceClient {

    @Override
    public BookingSummaryResponse updatePaymentStatus(Long id, BookingPaymentCallbackRequest request) {
        log.error("CRITICAL: BookingServiceClient fallback triggered for updatePaymentStatus: bookingId={}, txnRef={}, status={}",
                id, request.getTransactionReference(), request.getPaymentStatus());
        throw new BookingServiceUnavailableException(
                "Booking service is temporarily unavailable. Payment callback queued/compensation required for bookingId: " + id);
    }

    @Override
    public BookingSummaryResponse getBookingById(Long id) {
        log.error("CRITICAL: BookingServiceClient fallback triggered for getBookingById: bookingId={}", id);
        throw new BookingServiceUnavailableException(
                "Booking service is temporarily unavailable. Cannot verify bookingId: " + id);
    }
}


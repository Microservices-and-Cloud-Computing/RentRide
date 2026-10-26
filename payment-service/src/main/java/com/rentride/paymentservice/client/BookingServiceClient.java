package com.rentride.paymentservice.client;

import com.rentride.paymentservice.config.FeignConfig;
import com.rentride.paymentservice.dto.external.BookingPaymentCallbackRequest;
import com.rentride.paymentservice.dto.external.BookingSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * OpenFeign declarative HTTP client invoking remote booking-service endpoints.
 */
@FeignClient(
        name = "booking-service",
        url = "${booking-service.url:}",
        configuration = FeignConfig.class,
        fallback = BookingServiceClientFallback.class
)
public interface BookingServiceClient {

    /**
     * Updates the payment settlement status for a reservation in booking-service.
     *
     * @param id primary booking ID
     * @param request payment callback parameters
     * @return updated BookingSummaryResponse
     */
    @PatchMapping("/api/v1/bookings/{id}/payment-status")
    BookingSummaryResponse updatePaymentStatus(
            @PathVariable("id") Long id,
            @RequestBody BookingPaymentCallbackRequest request);

    /**
     * Retrieves current booking reservation specifications.
     *
     * @param id primary booking ID
     * @return BookingSummaryResponse containing reservation status and cost
     */
    @GetMapping("/api/v1/bookings/{id}")
    BookingSummaryResponse getBookingById(@PathVariable("id") Long id);
}


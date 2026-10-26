package com.rentride.paymentservice.client;

import com.rentride.paymentservice.exception.BookingNotFoundException;
import com.rentride.paymentservice.exception.BookingServiceUnavailableException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Custom error decoder translating OpenFeign HTTP error status codes into domain-specific exceptions.
 */
@Slf4j
@Component
public class CustomFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Feign RPC error on method={}: status={}", methodKey, response.status());

        switch (response.status()) {
            case 404:
                return new BookingNotFoundException("Booking not found in booking-service for call: " + methodKey);
            case 409:
                return new IllegalStateException("Booking is in an invalid state for payment confirmation");
            case 503:
                return new BookingServiceUnavailableException("Booking service is temporarily unavailable");
            default:
                return defaultDecoder.decode(methodKey, response);
        }
    }
}


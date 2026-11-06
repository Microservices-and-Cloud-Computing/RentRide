package com.rentride.bookingservice.client;

import com.rentride.bookingservice.exception.ServiceUnavailableException;
import com.rentride.bookingservice.exception.VehicleNotAvailableException;
import com.rentride.bookingservice.exception.VehicleNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom Feign ErrorDecoder translating HTTP status codes from vehicle-service
 * into strongly-typed domain exceptions.
 */
@Slf4j
public class CustomFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.warn("Feign invocation failed: methodKey={}, status={}, reason={}",
                methodKey, response.status(), response.reason());

        String resourceId = extractIdFromUrl(response.request() != null ? response.request().url() : null);

        return switch (response.status()) {
            case 404 -> new VehicleNotFoundException(
                    resourceId != null
                            ? "Vehicle not found with ID: " + resourceId
                            : "Vehicle not found"
            );
            case 409 -> new VehicleNotAvailableException(
                    "Vehicle is currently unavailable for booking"
            );
            case 503, 504 -> new ServiceUnavailableException(
                    "Vehicle service is temporarily unreachable"
            );
            default -> defaultDecoder.decode(methodKey, response);
        };
    }

    private String extractIdFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        // Extract numeric ID following "vehicles/" in the URL path (e.g., http://vehicle-service/api/v1/vehicles/10)
        String[] tokens = url.split("/");
        for (int i = 0; i < tokens.length; i++) {
            if ("vehicles".equalsIgnoreCase(tokens[i]) && i + 1 < tokens.length) {
                String candidate = tokens[i + 1].split("[?#]")[0];
                if (candidate.matches("\\d+")) {
                    return candidate;
                }
            }
        }
        return null;
    }
}


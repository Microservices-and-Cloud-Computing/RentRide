package com.rentride.bookingservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should map VehicleNotFoundException to 404 NOT_FOUND")
    void shouldMapVehicleNotFoundException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bookings");
        VehicleNotFoundException ex = new VehicleNotFoundException("Vehicle not found with ID: 10");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleVehicleNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("Vehicle not found with ID: 10");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/bookings");
    }

    @Test
    @DisplayName("Should map VehicleNotAvailableException to 409 CONFLICT")
    void shouldMapVehicleNotAvailableException() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bookings");
        VehicleNotAvailableException ex = new VehicleNotAvailableException("Vehicle is currently unavailable for booking");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleVehicleNotAvailable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage()).isEqualTo("Vehicle is currently unavailable for booking");
    }

    @Test
    @DisplayName("Should map ServiceUnavailableException to 503 SERVICE_UNAVAILABLE")
    void shouldMapServiceUnavailableException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bookings");
        ServiceUnavailableException ex = new ServiceUnavailableException("Vehicle service is temporarily unreachable");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleServiceUnavailable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(503);
        assertThat(response.getBody().getError()).isEqualTo("Service Unavailable");
        assertThat(response.getBody().getMessage()).isEqualTo("Vehicle service is temporarily unreachable");
    }

    @Test
    @DisplayName("Should map generic Exception to 500 INTERNAL_SERVER_ERROR")
    void shouldMapGenericException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bookings");
        RuntimeException ex = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected internal server error occurred");
    }
}


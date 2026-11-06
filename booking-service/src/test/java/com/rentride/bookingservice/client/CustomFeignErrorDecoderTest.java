package com.rentride.bookingservice.client;

import com.rentride.bookingservice.exception.ServiceUnavailableException;
import com.rentride.bookingservice.exception.VehicleNotAvailableException;
import com.rentride.bookingservice.exception.VehicleNotFoundException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CustomFeignErrorDecoder}.
 */
class CustomFeignErrorDecoderTest {

    private final CustomFeignErrorDecoder errorDecoder = new CustomFeignErrorDecoder();

    private Response createMockResponse(int status, String url) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                url,
                Collections.emptyMap(),
                Request.Body.empty(),
                null
        );

        return Response.builder()
                .status(status)
                .reason("Test Reason")
                .request(request)
                .headers(Collections.emptyMap())
                .body("{}".getBytes(StandardCharsets.UTF_8))
                .build();
    }

    @Test
    @DisplayName("Should decode 404 into VehicleNotFoundException with extracted ID")
    void shouldDecode404IntoVehicleNotFoundException() {
        Response response = createMockResponse(404, "http://vehicle-service/api/v1/vehicles/100");

        Exception exception = errorDecoder.decode("VehicleServiceClient#getVehicleById(Long)", response);

        assertThat(exception)
                .isInstanceOf(VehicleNotFoundException.class)
                .hasMessage("Vehicle not found with ID: 100");
    }

    @Test
    @DisplayName("Should decode 404 with availability URL into VehicleNotFoundException with extracted ID")
    void shouldDecode404AvailabilityUrl() {
        Response response = createMockResponse(404, "http://vehicle-service/api/v1/vehicles/205/availability");

        Exception exception = errorDecoder.decode("VehicleServiceClient#checkAvailability(Long)", response);

        assertThat(exception)
                .isInstanceOf(VehicleNotFoundException.class)
                .hasMessage("Vehicle not found with ID: 205");
    }

    @Test
    @DisplayName("Should decode 409 into VehicleNotAvailableException")
    void shouldDecode409IntoVehicleNotAvailableException() {
        Response response = createMockResponse(409, "http://vehicle-service/api/v1/vehicles/100/status");

        Exception exception = errorDecoder.decode("VehicleServiceClient#updateVehicleStatus(Long,VehicleStatusUpdateDto)", response);

        assertThat(exception)
                .isInstanceOf(VehicleNotAvailableException.class)
                .hasMessage("Vehicle is currently unavailable for booking");
    }

    @Test
    @DisplayName("Should decode 503 into ServiceUnavailableException")
    void shouldDecode503IntoServiceUnavailableException() {
        Response response = createMockResponse(503, "http://vehicle-service/api/v1/vehicles/100");

        Exception exception = errorDecoder.decode("VehicleServiceClient#getVehicleById(Long)", response);

        assertThat(exception)
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Vehicle service is temporarily unreachable");
    }

    @Test
    @DisplayName("Should decode 504 gateway timeout into ServiceUnavailableException")
    void shouldDecode504IntoServiceUnavailableException() {
        Response response = createMockResponse(504, "http://vehicle-service/api/v1/vehicles/100");

        Exception exception = errorDecoder.decode("VehicleServiceClient#getVehicleById(Long)", response);

        assertThat(exception)
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Vehicle service is temporarily unreachable");
    }
}


package com.rentride.bookingservice.client;

import com.rentride.bookingservice.dto.external.VehicleAvailabilityDto;
import com.rentride.bookingservice.dto.external.VehicleResponseDto;
import com.rentride.bookingservice.exception.ServiceUnavailableException;
import com.rentride.bookingservice.exception.VehicleNotFoundException;
import com.rentride.bookingservice.service.VehicleIntegrationService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Isolated integration tests for OpenFeign RPC communication, error decoding,
 * and service integration using a mock HTTP server.
 */
@SpringBootTest
@ActiveProfiles("test")
class VehicleServiceClientTest {

    private static HttpServer mockServer;
    private static int mockPort;

    @Autowired
    private VehicleIntegrationService vehicleIntegrationService;

    @BeforeAll
    static void startMockServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockPort = mockServer.getAddress().getPort();
        mockServer.start();
    }

    @AfterAll
    static void stopMockServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void configureFeignClient(DynamicPropertyRegistry registry) {
        registry.add("vehicle-service.url", () -> "http://localhost:" + mockPort);
    }

    @Test
    @DisplayName("1. testCheckAvailability_AvailableVehicle_ReturnsDto: WireMock/Mock 200 returns availability DTO")
    void testCheckAvailability_AvailableVehicle_ReturnsDto() {
        String path = "/api/v1/vehicles/1/availability";
        mockServer.createContext(path, exchange -> {
            String json = """
                    {
                        "vehicleId": 1,
                        "status": "AVAILABLE",
                        "isAvailable": true,
                        "dailyRentalRate": 8500.00
                    }
                    """;
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        VehicleAvailabilityDto response = vehicleIntegrationService.checkAvailability(1L);

        assertThat(response).isNotNull();
        assertThat(response.isAvailable()).isTrue();
        assertThat(response.getDailyRentalRate()).isEqualByComparingTo(new BigDecimal("8500.00"));
        assertThat(response.getStatus()).isEqualTo("AVAILABLE");
        assertThat(response.getVehicleId()).isEqualTo(1L);

        mockServer.removeContext(path);
    }

    @Test
    @DisplayName("2. testCheckAvailability_VehicleNotFound_ThrowsVehicleNotFoundException: WireMock/Mock 404 triggers VehicleNotFoundException")
    void testCheckAvailability_VehicleNotFound_ThrowsVehicleNotFoundException() {
        String path = "/api/v1/vehicles/999/availability";
        mockServer.createContext(path, exchange -> {
            String json = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Vehicle not found with ID: 999\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(404, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        assertThatThrownBy(() -> vehicleIntegrationService.checkAvailability(999L))
                .isInstanceOf(VehicleNotFoundException.class)
                .hasMessageContaining("Vehicle not found with ID: 999");

        mockServer.removeContext(path);
    }

    @Test
    @DisplayName("3. testUpdateVehicleStatus_Success: WireMock/Mock 200 PATCH updates status to RENTED")
    void testUpdateVehicleStatus_Success() {
        String path = "/api/v1/vehicles/1/status";
        mockServer.createContext(path, exchange -> {
            String json = """
                    {
                        "id": 1,
                        "providerId": 10,
                        "make": "Toyota",
                        "model": "Aqua",
                        "dailyRentalRate": 8500.00,
                        "status": "RENTED",
                        "location": "Colombo"
                    }
                    """;
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        VehicleResponseDto response = vehicleIntegrationService.updateVehicleStatus(1L, "RENTED", "Booking lock");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("RENTED");
        assertThat(response.getId()).isEqualTo(1L);

        mockServer.removeContext(path);
    }

    @Test
    @DisplayName("4. testVehicleServiceDown_FallbackOrExceptionTriggered: WireMock/Mock 503 triggers ServiceUnavailableException")
    void testVehicleServiceDown_FallbackOrExceptionTriggered() {
        String path = "/api/v1/vehicles/503/availability";
        mockServer.createContext(path, exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });

        assertThatThrownBy(() -> vehicleIntegrationService.checkAvailability(503L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("temporarily unreachable");

        mockServer.removeContext(path);
    }
}


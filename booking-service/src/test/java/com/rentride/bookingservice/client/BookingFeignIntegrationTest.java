package com.rentride.bookingservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.rentride.bookingservice.dto.BookingCreateRequest;
import com.rentride.bookingservice.entity.BookingStatus;
import com.rentride.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Declarative WireMock integration tests for OpenFeign RPC communication with vehicle-service (Commit 24).
 *
 * <p>Validates HTTP request transmission across network boundaries, headers, dynamic WireMock port mapping,
 * and exact JSON request payloads for inter-service status locking.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "vehicle-service.url=http://localhost:${wiremock.server.port}"
})
class BookingFeignIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        WireMock.reset();
    }

    @Test
    @DisplayName("Should create booking and verify exact WireMock HTTP payload transmitted to vehicle-service")
    void testCreateBooking_WithWireMock_VerifiesPayloadAndHeaders() throws Exception {
        // Arrange WireMock HTTP stubs
        stubFor(get(urlEqualTo("/api/v1/vehicles/10"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                {
                                  "id": 10,
                                  "providerId": 2,
                                  "make": "Honda",
                                  "model": "Civic",
                                  "dailyRentalRate": 8000.00,
                                  "status": "AVAILABLE",
                                  "licensePlateNumber": "WP-CBA-5566",
                                  "location": "Colombo",
                                  "categoryName": "SEDAN"
                                }
                                """)));

        stubFor(patch(urlEqualTo("/api/v1/vehicles/10/status"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                {
                                  "id": 10,
                                  "providerId": 2,
                                  "make": "Honda",
                                  "model": "Civic",
                                  "dailyRentalRate": 8000.00,
                                  "status": "RENTED",
                                  "licensePlateNumber": "WP-CBA-5566",
                                  "location": "Colombo",
                                  "categoryName": "SEDAN"
                                }
                                """)));

        BookingCreateRequest request = BookingCreateRequest.builder()
                .vehicleId(10L)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(5))
                .pickupNotes("Airport pickup")
                .build();

        // Act: Execute booking creation over MockMvc
        mockMvc.perform(post("/api/v1/bookings")
                        .header("X-User-Id", 50L)
                        .header("X-User-Role", "ROLE_CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.bookingReference").isNotEmpty())
                .andExpect(jsonPath("$.status").value(BookingStatus.PENDING_PAYMENT.name()))
                .andExpect(jsonPath("$.vehicleId").value(10))
                .andExpect(jsonPath("$.totalDays").value(3))
                .andExpect(jsonPath("$.dailyRate").value(8000.00))
                .andExpect(jsonPath("$.totalCost").value(24000.00))
                .andExpect(jsonPath("$.vehicleDetails.make").value("Honda"))
                .andExpect(jsonPath("$.vehicleDetails.model").value("Civic"));

        // Assert: Use WireMock's verify to confirm exact HTTP payload and headers transmitted across the network
        WireMock.verify(getRequestedFor(urlEqualTo("/api/v1/vehicles/10")));

        WireMock.verify(patchRequestedFor(urlEqualTo("/api/v1/vehicles/10/status"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath("$.status", equalTo("RENTED")))
                .withRequestBody(matchingJsonPath("$.reason", containing("Locked by Booking:"))));
    }
}


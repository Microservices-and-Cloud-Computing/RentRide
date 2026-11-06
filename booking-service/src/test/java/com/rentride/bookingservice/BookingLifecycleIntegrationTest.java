package com.rentride.bookingservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentride.bookingservice.client.VehicleServiceClient;
import com.rentride.bookingservice.dto.ReturnCondition;
import com.rentride.bookingservice.dto.VehiclePickupRequest;
import com.rentride.bookingservice.dto.VehicleReturnRequest;
import com.rentride.bookingservice.dto.external.VehicleResponseDto;
import com.rentride.bookingservice.dto.external.VehicleStatusUpdateDto;
import com.rentride.bookingservice.entity.Booking;
import com.rentride.bookingservice.entity.BookingStatus;
import com.rentride.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for vehicle pickup and return lifecycle state transitions (Commit 23).
 *
 * <p>Validates state machine transitions (CONFIRMED -> ACTIVE_RENTAL -> COMPLETED),
 * provider authorization checks, validation rules, and Feign synchronization with Vehicle-Service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @MockBean
    private VehicleServiceClient vehicleServiceClient;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
    }

    private Booking seedBooking(BookingStatus status, Long providerId, Long vehicleId) {
        Booking booking = Booking.builder()
                .bookingReference("RR-BK-" + System.currentTimeMillis())
                .customerId(10L)
                .providerId(providerId)
                .vehicleId(vehicleId)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(4))
                .totalDays(3)
                .dailyRate(new BigDecimal("8500.00"))
                .totalCost(new BigDecimal("25500.00"))
                .status(status)
                .additionalCharges(BigDecimal.ZERO)
                .build();
        return bookingRepository.save(booking);
    }

    @Test
    @DisplayName("testPickup_FromConfirmedState_Success: Moves booking to ACTIVE_RENTAL and populates actualPickupTime")
    void testPickup_FromConfirmedState_Success() throws Exception {
        Booking confirmedBooking = seedBooking(BookingStatus.CONFIRMED, 5L, 1L);

        VehicleResponseDto vehicleResponse = VehicleResponseDto.builder()
                .id(1L)
                .status("RENTED")
                .build();
        when(vehicleServiceClient.updateVehicleStatus(eq(1L), any(VehicleStatusUpdateDto.class)))
                .thenReturn(vehicleResponse);

        VehiclePickupRequest request = VehiclePickupRequest.builder()
                .odometerReading(45200.5)
                .fuelLevelPercent(100)
                .handoverNotes("Key handed over with full tank")
                .build();

        mockMvc.perform(post("/api/v1/bookings/{id}/pickup", confirmedBooking.getId())
                        .header("X-User-Id", 5L)
                        .header("X-User-Role", "ROLE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE_RENTAL"))
                .andExpect(jsonPath("$.actualPickupTime").isNotEmpty())
                .andExpect(jsonPath("$.vehicleStatusSummary").value("RENTED"));

        Booking updated = bookingRepository.findById(confirmedBooking.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(BookingStatus.ACTIVE_RENTAL);
        assertThat(updated.getActualPickupTime()).isNotNull();
        assertThat(updated.getStartOdometer()).isEqualTo(45200.5);
        assertThat(updated.getStartFuelPercent()).isEqualTo(100);
    }

    @Test
    @DisplayName("testPickup_FromPendingPaymentState_FailsWithConflict: Cannot pick up until booking is CONFIRMED")
    void testPickup_FromPendingPaymentState_FailsWithConflict() throws Exception {
        Booking pendingBooking = seedBooking(BookingStatus.PENDING_PAYMENT, 5L, 1L);

        VehiclePickupRequest request = VehiclePickupRequest.builder()
                .odometerReading(45200.5)
                .fuelLevelPercent(100)
                .handoverNotes("Attempt pickup early")
                .build();

        mockMvc.perform(post("/api/v1/bookings/{id}/pickup", pendingBooking.getId())
                        .header("X-User-Id", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("must be CONFIRMED")));

        Booking unaltered = bookingRepository.findById(pendingBooking.getId()).orElseThrow();
        assertThat(unaltered.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("testReturn_FromActiveRental_GoodCondition_SetsVehicleAvailable: Transitions to COMPLETED and syncs AVAILABLE")
    void testReturn_FromActiveRental_GoodCondition_SetsVehicleAvailable() throws Exception {
        Booking activeBooking = seedBooking(BookingStatus.ACTIVE_RENTAL, 5L, 1L);
        activeBooking.setActualPickupTime(LocalDateTime.now().minusDays(3));
        activeBooking.setStartOdometer(45000.0);
        activeBooking.setStartFuelPercent(100);
        bookingRepository.save(activeBooking);

        VehicleResponseDto vehicleResponse = VehicleResponseDto.builder()
                .id(1L)
                .status("AVAILABLE")
                .build();
        when(vehicleServiceClient.updateVehicleStatus(eq(1L), any(VehicleStatusUpdateDto.class)))
                .thenReturn(vehicleResponse);

        VehicleReturnRequest request = VehicleReturnRequest.builder()
                .odometerReading(45550.0)
                .fuelLevelPercent(95)
                .vehicleCondition(ReturnCondition.GOOD)
                .returnNotes("Returned on time, no damages found")
                .additionalCharges(BigDecimal.ZERO)
                .build();

        mockMvc.perform(post("/api/v1/bookings/{id}/return", activeBooking.getId())
                        .header("X-User-Id", 5L)
                        .header("X-User-Role", "ROLE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.actualReturnDate").isNotEmpty())
                .andExpect(jsonPath("$.vehicleStatusSummary").value("AVAILABLE"));

        verify(vehicleServiceClient).updateVehicleStatus(
                eq(1L),
                argThat(dto -> "AVAILABLE".equals(dto.getStatus()))
        );

        Booking completed = bookingRepository.findById(activeBooking.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(completed.getActualReturnDate()).isNotNull();
        assertThat(completed.getEndOdometer()).isEqualTo(45550.0);
        assertThat(completed.getEndFuelPercent()).isEqualTo(95);
    }

    @Test
    @DisplayName("testReturn_RequiresMaintenance_SetsVehicleMaintenance: Transitions to COMPLETED and syncs MAINTENANCE")
    void testReturn_RequiresMaintenance_SetsVehicleMaintenance() throws Exception {
        Booking activeBooking = seedBooking(BookingStatus.ACTIVE_RENTAL, 5L, 1L);
        activeBooking.setActualPickupTime(LocalDateTime.now().minusDays(3));
        activeBooking.setStartOdometer(45000.0);
        activeBooking.setStartFuelPercent(100);
        bookingRepository.save(activeBooking);

        VehicleResponseDto vehicleResponse = VehicleResponseDto.builder()
                .id(1L)
                .status("MAINTENANCE")
                .build();
        when(vehicleServiceClient.updateVehicleStatus(eq(1L), any(VehicleStatusUpdateDto.class)))
                .thenReturn(vehicleResponse);

        VehicleReturnRequest request = VehicleReturnRequest.builder()
                .odometerReading(45550.0)
                .fuelLevelPercent(80)
                .vehicleCondition(ReturnCondition.REQUIRES_MAINTENANCE)
                .returnNotes("Check engine light on, oil service required")
                .additionalCharges(new BigDecimal("2500.00"))
                .build();

        mockMvc.perform(post("/api/v1/bookings/{id}/return", activeBooking.getId())
                        .header("X-User-Id", 5L)
                        .header("X-User-Role", "ROLE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.vehicleStatusSummary").value("MAINTENANCE"))
                .andExpect(jsonPath("$.additionalCharges").value(2500.00));

        verify(vehicleServiceClient).updateVehicleStatus(
                eq(1L),
                argThat(dto -> "MAINTENANCE".equals(dto.getStatus()))
        );

        Booking completed = bookingRepository.findById(activeBooking.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(completed.getAdditionalCharges()).isEqualByComparingTo("2500.00");
    }

    @Test
    @DisplayName("testPickupAndReturn_UnauthorizedProvider_ReturnsForbidden: Blocks non-owners with 403")
    void testPickupAndReturn_UnauthorizedProvider_ReturnsForbidden() throws Exception {
        Booking confirmedBooking = seedBooking(BookingStatus.CONFIRMED, 5L, 1L);

        VehiclePickupRequest pickupRequest = VehiclePickupRequest.builder()
                .odometerReading(45200.5)
                .fuelLevelPercent(100)
                .build();

        // Attempt pickup as unauthorized provider 99
        mockMvc.perform(post("/api/v1/bookings/{id}/pickup", confirmedBooking.getId())
                        .header("X-User-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pickupRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("permission to manage handovers")));

        // Transition booking to ACTIVE_RENTAL directly to test return authorization
        confirmedBooking.setStatus(BookingStatus.ACTIVE_RENTAL);
        bookingRepository.save(confirmedBooking);

        VehicleReturnRequest returnRequest = VehicleReturnRequest.builder()
                .odometerReading(45550.0)
                .fuelLevelPercent(90)
                .vehicleCondition(ReturnCondition.GOOD)
                .build();

        // Attempt return as unauthorized provider 99
        mockMvc.perform(post("/api/v1/bookings/{id}/return", confirmedBooking.getId())
                        .header("X-User-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("permission to manage returns")));
    }
}


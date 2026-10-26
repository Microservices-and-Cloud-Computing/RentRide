package com.rentride.bookingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentride.bookingservice.dto.BookingCreateRequest;
import com.rentride.bookingservice.dto.BookingLifecycleResponse;
import com.rentride.bookingservice.dto.BookingResponse;
import com.rentride.bookingservice.dto.ReturnCondition;
import com.rentride.bookingservice.dto.VehiclePickupRequest;
import com.rentride.bookingservice.dto.VehicleReturnRequest;
import com.rentride.bookingservice.entity.BookingStatus;
import com.rentride.bookingservice.exception.BookingConflictException;
import com.rentride.bookingservice.exception.BookingNotFoundException;
import com.rentride.bookingservice.exception.GlobalExceptionHandler;
import com.rentride.bookingservice.exception.InvalidBookingStateException;
import com.rentride.bookingservice.exception.UnauthorizedBookingAccessException;
import com.rentride.bookingservice.exception.UnauthorizedProviderException;
import com.rentride.bookingservice.exception.VehicleUnavailableException;
import com.rentride.bookingservice.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
@Import(GlobalExceptionHandler.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @Test
    @DisplayName("POST /api/v1/bookings: Should return 201 Created on valid reservation request")
    void testCreateBooking_Success_Returns201() throws Exception {
        BookingCreateRequest request = BookingCreateRequest.builder()
                .vehicleId(1L)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(5))
                .pickupNotes("Airport pickup")
                .build();

        BookingResponse response = BookingResponse.builder()
                .id(100L)
                .bookingReference("RR-BK-20261026-TEST01")
                .customerId(50L)
                .providerId(10L)
                .vehicleId(1L)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalDays(3)
                .dailyRate(new BigDecimal("8000.00"))
                .totalCost(new BigDecimal("24000.00"))
                .status(BookingStatus.PENDING_PAYMENT)
                .build();

        when(bookingService.createBooking(eq(50L), any(BookingCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("X-User-Id", 50L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.bookingReference").value("RR-BK-20261026-TEST01"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalCost").value(24000.00));
    }

    @Test
    @DisplayName("POST /api/v1/bookings: Should return 400 Bad Request when X-User-Id header is missing")
    void testCreateBooking_MissingHeader_Returns400() throws Exception {
        BookingCreateRequest request = BookingCreateRequest.builder()
                .vehicleId(1L)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(5))
                .build();

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Missing required request header: X-User-Id"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings: Should return 400 Bad Request when bean validation fails")
    void testCreateBooking_BeanValidationFailure_Returns400() throws Exception {
        BookingCreateRequest request = BookingCreateRequest.builder()
                .startDate(LocalDate.now().minusDays(2))
                .endDate(LocalDate.now().minusDays(5))
                .build();

        mockMvc.perform(post("/api/v1/bookings")
                        .header("X-User-Id", 50L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/bookings: Should return 409 Conflict when vehicle is already booked")
    void testCreateBooking_Conflict_Returns409() throws Exception {
        BookingCreateRequest request = BookingCreateRequest.builder()
                .vehicleId(1L)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(5))
                .build();

        when(bookingService.createBooking(eq(50L), any(BookingCreateRequest.class)))
                .thenThrow(new BookingConflictException(1L, request.getStartDate(), request.getEndDate()));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("X-User-Id", 50L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings: Should return 409 Conflict when vehicle is unavailable")
    void testCreateBooking_VehicleUnavailable_Returns409() throws Exception {
        BookingCreateRequest request = BookingCreateRequest.builder()
                .vehicleId(1L)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(5))
                .build();

        when(bookingService.createBooking(eq(50L), any(BookingCreateRequest.class)))
                .thenThrow(new VehicleUnavailableException(1L, "MAINTENANCE"));

        mockMvc.perform(post("/api/v1/bookings")
                        .header("X-User-Id", 50L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /api/v1/bookings/{id}: Should return 200 OK with booking details")
    void testGetBookingById_Success_Returns200() throws Exception {
        BookingResponse response = BookingResponse.builder()
                .id(100L)
                .bookingReference("RR-BK-20261026-TEST01")
                .customerId(50L)
                .providerId(10L)
                .vehicleId(1L)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(5))
                .totalDays(3)
                .dailyRate(new BigDecimal("8000.00"))
                .totalCost(new BigDecimal("24000.00"))
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingService.getBookingById(eq(100L), eq(50L), eq("ROLE_CUSTOMER"))).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/100")
                        .header("X-User-Id", 50L)
                        .header("X-User-Role", "ROLE_CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.bookingReference").value("RR-BK-20261026-TEST01"));
    }

    @Test
    @DisplayName("GET /api/v1/bookings/{id}: Should return 404 Not Found when ID does not exist")
    void testGetBookingById_NotFound_Returns404() throws Exception {
        when(bookingService.getBookingById(eq(999L), eq(50L), any()))
                .thenThrow(new BookingNotFoundException(999L));

        mockMvc.perform(get("/api/v1/bookings/999")
                        .header("X-User-Id", 50L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Booking reservation not found with ID: 999"));
    }

    @Test
    @DisplayName("GET /api/v1/bookings/{id}: Should return 403 Forbidden on unauthorized access")
    void testGetBookingById_Unauthorized_Returns403() throws Exception {
        when(bookingService.getBookingById(eq(100L), eq(99L), eq("ROLE_CUSTOMER")))
                .thenThrow(new UnauthorizedBookingAccessException("You do not have permission to view reservation ID: 100"));

        mockMvc.perform(get("/api/v1/bookings/100")
                        .header("X-User-Id", 99L)
                        .header("X-User-Role", "ROLE_CUSTOMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("GET /api/v1/bookings/customer/my-bookings: Should return 200 OK with customer reservations")
    void testGetMyBookings_Success_Returns200() throws Exception {
        BookingResponse response = BookingResponse.builder()
                .id(100L)
                .bookingReference("RR-BK-20261026-TEST01")
                .customerId(50L)
                .providerId(10L)
                .vehicleId(1L)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(5))
                .totalDays(3)
                .dailyRate(new BigDecimal("8000.00"))
                .totalCost(new BigDecimal("24000.00"))
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingService.getBookingsByCustomerId(50L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/bookings/customer/my-bookings")
                        .header("X-User-Id", 50L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].bookingReference").value("RR-BK-20261026-TEST01"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{id}/confirm: Should return 200 OK with confirmed BookingResponse")
    void testGetBookingsByCustomerId_Success_Returns200() throws Exception {
        BookingResponse response = BookingResponse.builder()
                .id(100L)
                .bookingReference("RR-BK-20261026-TEST01")
                .customerId(50L)
                .providerId(10L)
                .vehicleId(1L)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(5))
                .totalDays(3)
                .dailyRate(new BigDecimal("8000.00"))
                .totalCost(new BigDecimal("24000.00"))
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingService.getBookingsByCustomerId(50L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/bookings/customer/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].bookingReference").value("RR-BK-20261026-TEST01"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{id}/confirm: Should return 200 OK with confirmed BookingResponse")
    void testConfirmBooking_Success_Returns200() throws Exception {
        BookingResponse response = BookingResponse.builder()
                .id(100L)
                .bookingReference("RR-BK-20261026-TEST01")
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingService.confirmBooking(100L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings/100/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{id}/pickup: Should return 200 OK with BookingLifecycleResponse")
    void testRecordPickup_Success_Returns200() throws Exception {
        VehiclePickupRequest request = VehiclePickupRequest.builder()
                .odometerReading(45200.0)
                .fuelLevelPercent(100)
                .handoverNotes("Key given with original documents")
                .build();

        BookingLifecycleResponse response = BookingLifecycleResponse.builder()
                .id(100L)
                .bookingReference("RR-BK-20261026-TEST01")
                .customerId(50L)
                .providerId(10L)
                .vehicleId(1L)
                .status(BookingStatus.ACTIVE_RENTAL)
                .actualPickupTime(LocalDateTime.now())
                .vehicleStatusSummary("RENTED")
                .startOdometer(45200.0)
                .startFuelPercent(100)
                .build();

        when(bookingService.recordVehiclePickup(eq(100L), eq(10L), any(VehiclePickupRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings/100/pickup")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE_RENTAL"))
                .andExpect(jsonPath("$.startOdometer").value(45200.0))
                .andExpect(jsonPath("$.vehicleStatusSummary").value("RENTED"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{id}/pickup: Should return 409 Conflict when state is invalid")
    void testRecordPickup_InvalidState_Returns409() throws Exception {
        VehiclePickupRequest request = VehiclePickupRequest.builder()
                .odometerReading(45200.0)
                .fuelLevelPercent(100)
                .build();

        when(bookingService.recordVehiclePickup(eq(100L), eq(10L), any(VehiclePickupRequest.class)))
                .thenThrow(new InvalidBookingStateException("Vehicle pickup cannot be recorded: current status is PENDING_PAYMENT (must be CONFIRMED)"));

        mockMvc.perform(post("/api/v1/bookings/100/pickup")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Vehicle pickup cannot be recorded: current status is PENDING_PAYMENT (must be CONFIRMED)"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{id}/pickup: Should return 403 Forbidden when unauthorized provider")
    void testRecordPickup_UnauthorizedProvider_Returns403() throws Exception {
        VehiclePickupRequest request = VehiclePickupRequest.builder()
                .odometerReading(45200.0)
                .fuelLevelPercent(100)
                .build();

        when(bookingService.recordVehiclePickup(eq(100L), eq(99L), any(VehiclePickupRequest.class)))
                .thenThrow(new UnauthorizedProviderException("You do not have permission to manage handovers for booking ID: 100"));

        mockMvc.perform(post("/api/v1/bookings/100/pickup")
                        .header("X-User-Id", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{id}/return: Should return 200 OK with BookingLifecycleResponse")
    void testRecordReturn_Success_Returns200() throws Exception {
        VehicleReturnRequest request = VehicleReturnRequest.builder()
                .odometerReading(45600.0)
                .fuelLevelPercent(100)
                .vehicleCondition(ReturnCondition.GOOD)
                .returnNotes("Returned in excellent condition")
                .additionalCharges(BigDecimal.ZERO)
                .build();

        BookingLifecycleResponse response = BookingLifecycleResponse.builder()
                .id(100L)
                .bookingReference("RR-BK-20261026-TEST01")
                .customerId(50L)
                .providerId(10L)
                .vehicleId(1L)
                .status(BookingStatus.COMPLETED)
                .actualReturnDate(LocalDateTime.now())
                .vehicleStatusSummary("AVAILABLE")
                .endOdometer(45600.0)
                .endFuelPercent(100)
                .totalCost(new BigDecimal("24000.00"))
                .additionalCharges(BigDecimal.ZERO)
                .finalAmountPaid(new BigDecimal("24000.00"))
                .build();

        when(bookingService.recordVehicleReturn(eq(100L), eq(10L), any(VehicleReturnRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings/100/return")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.vehicleStatusSummary").value("AVAILABLE"))
                .andExpect(jsonPath("$.finalAmountPaid").value(24000.00));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{id}/return: Should return 409 Conflict when state is invalid")
    void testRecordReturn_InvalidState_Returns409() throws Exception {
        VehicleReturnRequest request = VehicleReturnRequest.builder()
                .odometerReading(45600.0)
                .fuelLevelPercent(100)
                .vehicleCondition(ReturnCondition.GOOD)
                .build();

        when(bookingService.recordVehicleReturn(eq(100L), eq(10L), any(VehicleReturnRequest.class)))
                .thenThrow(new InvalidBookingStateException("Vehicle return cannot be processed: current status is CONFIRMED (must be ACTIVE_RENTAL)"));

        mockMvc.perform(post("/api/v1/bookings/100/return")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}

package com.rentride.vehicleservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentride.vehicleservice.dto.VehicleCreateRequest;
import com.rentride.vehicleservice.dto.VehicleResponse;
import com.rentride.vehicleservice.dto.VehicleUpdateRequest;
import com.rentride.vehicleservice.entity.FuelType;
import com.rentride.vehicleservice.entity.TransmissionType;
import com.rentride.vehicleservice.entity.VehicleStatus;
import com.rentride.vehicleservice.exception.DuplicateResourceException;
import com.rentride.vehicleservice.exception.GlobalExceptionHandler;
import com.rentride.vehicleservice.exception.ResourceNotFoundException;
import com.rentride.vehicleservice.exception.UnauthorizedAccessException;
import com.rentride.vehicleservice.service.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc Slice tests for {@link VehicleController}.
 *
 * <p>Verifies request header processing (X-User-Id, X-User-Role), validation constraints,
 * response statuses, and exception mapping via {@link GlobalExceptionHandler}.
 */
@WebMvcTest(controllers = VehicleController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VehicleService vehicleService;

    @Test
    @DisplayName("testCreateVehicle_Success: Valid request with X-User-Id header returns 201 Created and response body")
    void testCreateVehicle_Success() throws Exception {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .make("Toyota")
                .model("Aqua")
                .manufacturingYear(2021)
                .licensePlateNumber("WP-CBA-9090")
                .categoryId(1L)
                .seatingCapacity(5)
                .fuelType(FuelType.HYBRID)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("8500.00"))
                .location("Colombo")
                .imageUrl("https://images.rentride.com/aqua.jpg")
                .build();

        VehicleResponse response = VehicleResponse.builder()
                .id(1L)
                .providerId(5L)
                .make("Toyota")
                .model("Aqua")
                .manufacturingYear(2021)
                .licensePlateNumber("WP-CBA-9090")
                .categoryId(1L)
                .categoryName("SEDAN")
                .seatingCapacity(5)
                .fuelType(FuelType.HYBRID)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("8500.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Colombo")
                .imageUrl("https://images.rentride.com/aqua.jpg")
                .createdAt(LocalDateTime.now())
                .build();

        when(vehicleService.createVehicle(eq(5L), any(VehicleCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("X-User-Id", 5L)
                        .header("X-User-Role", "ROLE_PROVIDER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.providerId", is(5)))
                .andExpect(jsonPath("$.make", is("Toyota")))
                .andExpect(jsonPath("$.model", is("Aqua")))
                .andExpect(jsonPath("$.licensePlateNumber", is("WP-CBA-9090")))
                .andExpect(jsonPath("$.status", is("AVAILABLE")))
                .andExpect(jsonPath("$.dailyRentalRate", is(8500.00)))
                .andExpect(jsonPath("$.location", is("Colombo")));
    }

    @Test
    @DisplayName("testCreateVehicle_DuplicateLicensePlate_Returns409: Duplicate plate number returns 409 Conflict")
    void testCreateVehicle_DuplicateLicensePlate_Returns409() throws Exception {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .make("Toyota")
                .model("Aqua")
                .manufacturingYear(2021)
                .licensePlateNumber("WP-CBA-9090")
                .categoryId(1L)
                .seatingCapacity(5)
                .fuelType(FuelType.HYBRID)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("8500.00"))
                .location("Colombo")
                .build();

        when(vehicleService.createVehicle(eq(5L), any(VehicleCreateRequest.class)))
                .thenThrow(new DuplicateResourceException("Vehicle with license plate number 'WP-CBA-9090' already exists"));

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("X-User-Id", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("Vehicle with license plate number 'WP-CBA-9090' already exists")));
    }

    @Test
    @DisplayName("testUpdateVehicle_UnauthorizedProvider_Returns403or400: Modification by unauthorized provider returns 403 Forbidden")
    void testUpdateVehicle_UnauthorizedProvider_Returns403or400() throws Exception {
        VehicleUpdateRequest request = VehicleUpdateRequest.builder()
                .dailyRentalRate(new BigDecimal("9000.00"))
                .location("Colombo")
                .seatingCapacity(5)
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleService.updateVehicle(eq(1L), eq(9L), any(VehicleUpdateRequest.class)))
                .thenThrow(new UnauthorizedAccessException("Provider 9 is not authorized to modify vehicle 1"));

        mockMvc.perform(put("/api/v1/vehicles/1")
                        .header("X-User-Id", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Provider 9 is not authorized to modify vehicle 1")));
    }

    @Test
    @DisplayName("testUpdateVehicle_Success: Authorized update returns 200 OK and updated payload")
    void testUpdateVehicle_Success() throws Exception {
        VehicleUpdateRequest request = VehicleUpdateRequest.builder()
                .dailyRentalRate(new BigDecimal("9000.00"))
                .location("Colombo")
                .seatingCapacity(5)
                .status(VehicleStatus.AVAILABLE)
                .build();

        VehicleResponse updatedResponse = VehicleResponse.builder()
                .id(1L)
                .providerId(10L)
                .make("Toyota")
                .model("Aqua")
                .dailyRentalRate(new BigDecimal("9000.00"))
                .location("Colombo")
                .seatingCapacity(5)
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleService.updateVehicle(eq(1L), eq(10L), any(VehicleUpdateRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/vehicles/1")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyRentalRate", is(9000.00)))
                .andExpect(jsonPath("$.location", is("Colombo")));
    }

    @Test
    @DisplayName("testGetVehiclesByProvider_ReturnsList: Request to /provider/my-fleet with X-User-Id returns provider fleet")
    void testGetVehiclesByProvider_ReturnsList() throws Exception {
        VehicleResponse v1 = VehicleResponse.builder()
                .id(1L)
                .providerId(5L)
                .make("Toyota")
                .model("Prius")
                .licensePlateNumber("WP-CAB-1111")
                .status(VehicleStatus.AVAILABLE)
                .dailyRentalRate(new BigDecimal("5000.00"))
                .build();

        VehicleResponse v2 = VehicleResponse.builder()
                .id(2L)
                .providerId(5L)
                .make("Honda")
                .model("Fit")
                .licensePlateNumber("WP-CAB-2222")
                .status(VehicleStatus.RENTED)
                .dailyRentalRate(new BigDecimal("4500.00"))
                .build();

        when(vehicleService.getVehiclesByProvider(5L)).thenReturn(List.of(v1, v2));

        mockMvc.perform(get("/api/v1/vehicles/provider/my-fleet")
                        .header("X-User-Id", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].providerId", is(5)))
                .andExpect(jsonPath("$[0].licensePlateNumber", is("WP-CAB-1111")))
                .andExpect(jsonPath("$[1].providerId", is(5)))
                .andExpect(jsonPath("$[1].licensePlateNumber", is("WP-CAB-2222")));
    }

    @Test
    @DisplayName("testGetVehicleById_Success: Public query returns vehicle details")
    void testGetVehicleById_Success() throws Exception {
        VehicleResponse response = VehicleResponse.builder()
                .id(1L)
                .providerId(10L)
                .make("Toyota")
                .model("Aqua")
                .licensePlateNumber("WP-CBA-9090")
                .status(VehicleStatus.AVAILABLE)
                .dailyRentalRate(new BigDecimal("8500.00"))
                .build();

        when(vehicleService.getVehicleById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/vehicles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.make", is("Toyota")))
                .andExpect(jsonPath("$.licensePlateNumber", is("WP-CBA-9090")));
    }

    @Test
    @DisplayName("testGetVehicleById_NotFound_Returns404: Non-existent ID returns 404 Not Found")
    void testGetVehicleById_NotFound_Returns404() throws Exception {
        when(vehicleService.getVehicleById(999L))
                .thenThrow(new ResourceNotFoundException("Vehicle not found with id: 999"));

        mockMvc.perform(get("/api/v1/vehicles/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Vehicle not found with id: 999")));
    }

    @Test
    @DisplayName("testDeleteVehicle_Success: Authorized delete returns 204 No Content")
    void testDeleteVehicle_Success() throws Exception {
        doNothing().when(vehicleService).deleteVehicle(1L, 10L);

        mockMvc.perform(delete("/api/v1/vehicles/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("testDeleteVehicle_UnauthorizedProvider_Returns403: Delete by non-owner returns 403 Forbidden")
    void testDeleteVehicle_UnauthorizedProvider_Returns403() throws Exception {
        doThrow(new UnauthorizedAccessException("Provider 9 is not authorized to delete vehicle 1"))
                .when(vehicleService).deleteVehicle(1L, 9L);

        mockMvc.perform(delete("/api/v1/vehicles/1")
                        .header("X-User-Id", 9L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("testCreateVehicle_ValidationFailure_Returns400: Invalid fields return 400 Bad Request with field errors")
    void testCreateVehicle_ValidationFailure_Returns400() throws Exception {
        VehicleCreateRequest invalidRequest = VehicleCreateRequest.builder()
                .make("")
                .model("")
                .manufacturingYear(1990) // earlier than 2000
                .dailyRentalRate(new BigDecimal("-10.00")) // negative
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("X-User-Id", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.validationErrors", notNullValue()));
    }
}

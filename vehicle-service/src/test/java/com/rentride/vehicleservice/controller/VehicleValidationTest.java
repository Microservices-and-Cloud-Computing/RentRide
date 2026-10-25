package com.rentride.vehicleservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentride.vehicleservice.dto.VehicleCreateRequest;
import com.rentride.vehicleservice.entity.FuelType;
import com.rentride.vehicleservice.entity.TransmissionType;
import com.rentride.vehicleservice.exception.GlobalExceptionHandler;
import com.rentride.vehicleservice.service.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Targeted validation and deserialization error tests for Vehicle endpoints.
 */
@WebMvcTest(VehicleController.class)
@Import(GlobalExceptionHandler.class)
class VehicleValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VehicleService vehicleService;

    @ParameterizedTest
    @ValueSource(strings = {"WP-CBA-1234", "WP-CA-1001", "65-4321", "CAB-1234", "WP-1234"})
    @DisplayName("Valid Sri Lankan license plates pass validation")
    void testValidLicensePlateFormats(String plate) throws Exception {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .make("Toyota")
                .model("Corolla")
                .manufacturingYear(2022)
                .licensePlateNumber(plate)
                .categoryId(1L)
                .seatingCapacity(5)
                .fuelType(FuelType.PETROL)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("6000.00"))
                .location("Colombo")
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "1234", "WP--1234", "WP-CBA-12345", "toolongplatename123"})
    @DisplayName("Invalid license plates fail validation with 400 Bad Request")
    void testInvalidLicensePlateFormats(String plate) throws Exception {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .make("Toyota")
                .model("Corolla")
                .manufacturingYear(2022)
                .licensePlateNumber(plate)
                .categoryId(1L)
                .seatingCapacity(5)
                .fuelType(FuelType.PETROL)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("6000.00"))
                .location("Colombo")
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.errors.licensePlateNumber", containsString("Sri Lankan vehicle registration plate format")));
    }

    @Test
    @DisplayName("Daily rate below 500.00 triggers validation error")
    void testDailyRentalRateBelowMinimum() throws Exception {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .make("Toyota")
                .model("Corolla")
                .manufacturingYear(2022)
                .licensePlateNumber("WP-CBA-1234")
                .categoryId(1L)
                .seatingCapacity(5)
                .fuelType(FuelType.PETROL)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("499.99"))
                .location("Colombo")
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.errors.dailyRentalRate", containsString("cannot be less than 500.00")));
    }

    @Test
    @DisplayName("Manufacturing year before 2000 triggers validation error")
    void testManufacturingYearBefore2000() throws Exception {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .make("Toyota")
                .model("Corolla")
                .manufacturingYear(1999)
                .licensePlateNumber("WP-CBA-1234")
                .categoryId(1L)
                .seatingCapacity(5)
                .fuelType(FuelType.PETROL)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("6000.00"))
                .location("Colombo")
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.errors.manufacturingYear", containsString("2000 or newer")));
    }

    @Test
    @DisplayName("Seating capacity exceeding 60 triggers validation error")
    void testSeatingCapacityExceedsMax() throws Exception {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .make("Toyota")
                .model("Coaster")
                .manufacturingYear(2021)
                .licensePlateNumber("WP-CBA-1234")
                .categoryId(1L)
                .seatingCapacity(61)
                .fuelType(FuelType.DIESEL)
                .transmission(TransmissionType.MANUAL)
                .dailyRentalRate(new BigDecimal("12000.00"))
                .location("Colombo")
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.errors.seatingCapacity", notNullValue()));
    }

    @Test
    @DisplayName("Location with length 1 triggers validation error")
    void testLocationTooShort() throws Exception {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .make("Toyota")
                .model("Corolla")
                .manufacturingYear(2022)
                .licensePlateNumber("WP-CBA-1234")
                .categoryId(1L)
                .seatingCapacity(5)
                .fuelType(FuelType.PETROL)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("6000.00"))
                .location("A")
                .build();

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation Failed")))
                .andExpect(jsonPath("$.errors.location", notNullValue()));
    }

    @Test
    @DisplayName("Malformed enum value FuelType: NUCLEAR returns clean 400 Bad Request")
    void testInvalidEnumValue_ReturnsClean400() throws Exception {
        String jsonPayload = """
                {
                    "make": "Toyota",
                    "model": "Corolla",
                    "manufacturingYear": 2022,
                    "licensePlateNumber": "WP-CBA-1234",
                    "categoryId": 1,
                    "seatingCapacity": 5,
                    "fuelType": "NUCLEAR",
                    "transmission": "AUTOMATIC",
                    "dailyRentalRate": 6000.00,
                    "location": "Colombo"
                }
                """;

        mockMvc.perform(post("/api/v1/vehicles")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("NUCLEAR")))
                .andExpect(jsonPath("$.message", containsString("PETROL")));
    }
}


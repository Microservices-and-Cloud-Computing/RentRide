package com.rentride.vehicleservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentride.vehicleservice.dto.VehicleStatusUpdateRequest;
import com.rentride.vehicleservice.entity.Category;
import com.rentride.vehicleservice.entity.FuelType;
import com.rentride.vehicleservice.entity.TransmissionType;
import com.rentride.vehicleservice.entity.Vehicle;
import com.rentride.vehicleservice.entity.VehicleStatus;
import com.rentride.vehicleservice.repository.CategoryRepository;
import com.rentride.vehicleservice.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration and Concurrency test suite for Commit 17 internal vehicle status endpoints.
 *
 * <p>Validates state transitions (AVAILABLE &rarr; RENTED &rarr; AVAILABLE &rarr; MAINTENANCE),
 * invalid transition conflict handling (HTTP 409), lightweight availability queries,
 * and pessimistic locking race condition prevention.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class VehicleStatusUpdateTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category testCategory;
    private Vehicle testVehicle;

    @BeforeEach
    void setUp() {
        vehicleRepository.deleteAll();
        categoryRepository.deleteAll();

        testCategory = categoryRepository.save(Category.builder()
                .name("SEDAN")
                .description("Standard sedan category")
                .baseDailyRate(new BigDecimal("45.00"))
                .build());

        testVehicle = vehicleRepository.save(Vehicle.builder()
                .providerId(1L)
                .make("Toyota")
                .model("Camry")
                .manufacturingYear(2023)
                .licensePlateNumber("WP-CA-1001")
                .seatingCapacity(5)
                .fuelType(FuelType.PETROL)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("95.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Colombo")
                .category(testCategory)
                .build());
    }

    @Test
    @DisplayName("testUpdateStatus_AvailableToRented_Success: Transition AVAILABLE to RENTED returns 200 OK")
    void testUpdateStatus_AvailableToRented_Success() throws Exception {
        VehicleStatusUpdateRequest request = VehicleStatusUpdateRequest.builder()
                .status(VehicleStatus.RENTED)
                .reason("Booking #101 confirmed")
                .build();

        mockMvc.perform(patch("/api/v1/vehicles/" + testVehicle.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testVehicle.getId().intValue())))
                .andExpect(jsonPath("$.status", is("RENTED")));

        Vehicle reloaded = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(VehicleStatus.RENTED);
    }

    @Test
    @DisplayName("testUpdateStatus_RentedToAvailable_Success: Transition RENTED to AVAILABLE returns 200 OK")
    void testUpdateStatus_RentedToAvailable_Success() throws Exception {
        testVehicle.setStatus(VehicleStatus.RENTED);
        vehicleRepository.save(testVehicle);

        VehicleStatusUpdateRequest request = VehicleStatusUpdateRequest.builder()
                .status(VehicleStatus.AVAILABLE)
                .reason("Returned in good condition")
                .build();

        mockMvc.perform(patch("/api/v1/vehicles/" + testVehicle.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testVehicle.getId().intValue())))
                .andExpect(jsonPath("$.status", is("AVAILABLE")));

        Vehicle reloaded = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    @DisplayName("testUpdateStatus_InvalidTransition_ThrowsConflict: Re-renting RENTED vehicle throws 409 Conflict")
    void testUpdateStatus_InvalidTransition_ThrowsConflict() throws Exception {
        testVehicle.setStatus(VehicleStatus.RENTED);
        vehicleRepository.save(testVehicle);

        VehicleStatusUpdateRequest request = VehicleStatusUpdateRequest.builder()
                .status(VehicleStatus.RENTED)
                .reason("Attempt duplicate rental")
                .build();

        mockMvc.perform(patch("/api/v1/vehicles/" + testVehicle.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("Vehicle cannot transition from RENTED to RENTED")));
    }

    @Test
    @DisplayName("testCheckAvailability_ReturnsTrueForAvailableVehicle: Returns isAvailable=true and rate")
    void testCheckAvailability_ReturnsTrueForAvailableVehicle() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/" + testVehicle.getId() + "/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId", is(testVehicle.getId().intValue())))
                .andExpect(jsonPath("$.status", is("AVAILABLE")))
                .andExpect(jsonPath("$.isAvailable", is(true)))
                .andExpect(jsonPath("$.dailyRentalRate", is(95.00)));
    }

    @Test
    @DisplayName("testConcurrentStatusUpdate_PreventsRaceCondition: Exactly one booking request succeeds, other rejected with 409 Conflict")
    void testConcurrentStatusUpdate_PreventsRaceCondition() throws Exception {
        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Integer> responseStatuses = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            final int requestId = i + 1;
            futures.add(executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    // Block until all threads are ready to fire simultaneously
                    startLatch.await();

                    VehicleStatusUpdateRequest request = VehicleStatusUpdateRequest.builder()
                            .status(VehicleStatus.RENTED)
                            .reason("Concurrent booking request #" + requestId)
                            .build();

                    MvcResult result = mockMvc.perform(patch("/api/v1/vehicles/" + testVehicle.getId() + "/status")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn();

                    responseStatuses.add(result.getResponse().getStatus());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        // Wait for both worker threads to reach ready barrier
        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        // Fire both threads simultaneously
        startLatch.countDown();

        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Exactly one request must succeed (HTTP 200) and the second must be rejected (HTTP 409)
        assertThat(responseStatuses).containsExactlyInAnyOrder(200, 409);

        // Verify final persistent status in database
        Vehicle finalVehicle = vehicleRepository.findById(testVehicle.getId()).orElseThrow();
        assertThat(finalVehicle.getStatus()).isEqualTo(VehicleStatus.RENTED);
    }
}


package com.rentride.vehicleservice.controller;

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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for dynamic vehicle search and filtering endpoint.
 *
 * <p>Tests inventory search by location, price range, category, status exclusion,
 * pagination, and sorting against an embedded database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VehicleSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category sedanCategory;
    private Category suvCategory;

    @BeforeEach
    void setUp() {
        vehicleRepository.deleteAll();
        categoryRepository.deleteAll();

        sedanCategory = categoryRepository.save(Category.builder()
                .name("SEDAN")
                .description("Economy Sedan")
                .baseDailyRate(new BigDecimal("45.00"))
                .build());

        suvCategory = categoryRepository.save(Category.builder()
                .name("SUV")
                .description("Compact SUV")
                .baseDailyRate(new BigDecimal("75.00"))
                .build());

        // Vehicle A: SEDAN, Colombo, Rate: 5,000, Status: AVAILABLE
        vehicleRepository.save(Vehicle.builder()
                .providerId(10L)
                .make("Toyota")
                .model("Corolla")
                .manufacturingYear(2021)
                .licensePlateNumber("WP-AAA-1001")
                .category(sedanCategory)
                .seatingCapacity(5)
                .fuelType(FuelType.PETROL)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("5000.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Colombo")
                .build());

        // Vehicle B: SEDAN, Kandy, Rate: 7,500, Status: AVAILABLE
        vehicleRepository.save(Vehicle.builder()
                .providerId(10L)
                .make("Toyota")
                .model("Premio")
                .manufacturingYear(2020)
                .licensePlateNumber("CP-BBB-2002")
                .category(sedanCategory)
                .seatingCapacity(5)
                .fuelType(FuelType.PETROL)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("7500.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Kandy")
                .build());

        // Vehicle C: SUV, Colombo, Rate: 12,000, Status: AVAILABLE
        vehicleRepository.save(Vehicle.builder()
                .providerId(20L)
                .make("Toyota")
                .model("RAV4")
                .manufacturingYear(2022)
                .licensePlateNumber("WP-CCC-3003")
                .category(suvCategory)
                .seatingCapacity(5)
                .fuelType(FuelType.HYBRID)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("12000.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Colombo")
                .build());

        // Vehicle D: SUV, Colombo, Rate: 15,000, Status: RENTED (should never appear in customer search)
        vehicleRepository.save(Vehicle.builder()
                .providerId(20L)
                .make("Toyota")
                .model("Harrier")
                .manufacturingYear(2023)
                .licensePlateNumber("WP-DDD-4004")
                .category(suvCategory)
                .seatingCapacity(5)
                .fuelType(FuelType.HYBRID)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("15000.00"))
                .status(VehicleStatus.RENTED)
                .location("Colombo")
                .build());
    }

    @Test
    @DisplayName("testSearch_ByLocation_ReturnsOnlyMatchingLocation: Colombo query returns Vehicle A and C, excludes B and non-available D")
    void testSearch_ByLocation_ReturnsOnlyMatchingLocation() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/search")
                        .param("location", "Colombo")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].location", is("Colombo")))
                .andExpect(jsonPath("$.content[1].location", is("Colombo")));
    }

    @Test
    @DisplayName("testSearch_ByPriceRange_ReturnsWithinBounds: Rate between 6000 and 10000 returns only Vehicle B")
    void testSearch_ByPriceRange_ReturnsWithinBounds() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/search")
                        .param("minPrice", "6000")
                        .param("maxPrice", "10000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].licensePlateNumber", is("CP-BBB-2002")))
                .andExpect(jsonPath("$.content[0].dailyRentalRate", is(7500.00)));
    }

    @Test
    @DisplayName("testSearch_CombinedCategoryAndPriceFilter: Sedan category and maxPrice 6000 returns only Vehicle A")
    void testSearch_CombinedCategoryAndPriceFilter() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/search")
                        .param("categoryId", sedanCategory.getId().toString())
                        .param("maxPrice", "6000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].licensePlateNumber", is("WP-AAA-1001")))
                .andExpect(jsonPath("$.content[0].categoryName", is("SEDAN")))
                .andExpect(jsonPath("$.content[0].dailyRentalRate", is(5000.00)));
    }

    @Test
    @DisplayName("testSearch_SortingByRateDescending: Sort by rate desc returns Vehicle C (12,000), B (7,500), A (5,000)")
    void testSearch_SortingByRateDescending() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/search")
                        .param("sortBy", "dailyRentalRate")
                        .param("sortDir", "desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].licensePlateNumber", is("WP-CCC-3003")))
                .andExpect(jsonPath("$.content[0].dailyRentalRate", is(12000.00)))
                .andExpect(jsonPath("$.content[1].licensePlateNumber", is("CP-BBB-2002")))
                .andExpect(jsonPath("$.content[1].dailyRentalRate", is(7500.00)))
                .andExpect(jsonPath("$.content[2].licensePlateNumber", is("WP-AAA-1001")))
                .andExpect(jsonPath("$.content[2].dailyRentalRate", is(5000.00)));
    }

    @Test
    @DisplayName("testSearch_ExcludesNonAvailableVehicles: Vehicle D (RENTED) is excluded regardless of filter parameters")
    void testSearch_ExcludesNonAvailableVehicles() throws Exception {
        // Querying SUV category in Colombo without price restrictions
        mockMvc.perform(get("/api/v1/vehicles/search")
                        .param("categoryId", suvCategory.getId().toString())
                        .param("location", "Colombo")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Only Vehicle C is returned (AVAILABLE), Vehicle D (RENTED) is excluded
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].licensePlateNumber", is("WP-CCC-3003")))
                .andExpect(jsonPath("$.content[0].status", is("AVAILABLE")));
    }

    @Test
    @DisplayName("testSearch_Pagination_LimitsPageSizeAndReturnsCorrectMetadata")
    void testSearch_Pagination_LimitsPageSizeAndReturnsCorrectMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles/search")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "dailyRentalRate")
                        .param("sortDir", "asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.pageSize", is(2)))
                .andExpect(jsonPath("$.pageNo", is(0)))
                .andExpect(jsonPath("$.last", is(false)))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }
}

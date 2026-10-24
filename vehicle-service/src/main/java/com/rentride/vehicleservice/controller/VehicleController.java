package com.rentride.vehicleservice.controller;

import com.rentride.vehicleservice.dto.PaginatedResponse;
import com.rentride.vehicleservice.dto.VehicleCreateRequest;
import com.rentride.vehicleservice.dto.VehicleResponse;
import com.rentride.vehicleservice.dto.VehicleSearchCriteria;
import com.rentride.vehicleservice.dto.VehicleUpdateRequest;
import com.rentride.vehicleservice.entity.FuelType;
import com.rentride.vehicleservice.entity.TransmissionType;
import com.rentride.vehicleservice.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * REST controller exposing endpoints for vehicle inventory CRUD operations,
 * public vehicle catalog lookups, and rental provider fleet management.
 *
 * <p>Extracts caller identity context (such as {@code X-User-Id} and {@code X-User-Role})
 * forwarded from the API Gateway authentication filter.
 */
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Slf4j
public class VehicleController {

    private final VehicleService vehicleService;

    /**
     * Registers a new vehicle in the provider's fleet.
     *
     * @param providerId caller ID extracted from header X-User-Id
     * @param userRole optional role header for logging and audit tracking
     * @param request vehicle creation payload
     * @return 201 Created with persisted VehicleResponse
     */
    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(
            @RequestHeader("X-User-Id") Long providerId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody VehicleCreateRequest request) {

        log.info("REST POST /api/v1/vehicles: providerId={}, role={}, make={}, model={}",
                providerId, userRole, request.getMake(), request.getModel());

        VehicleResponse response = vehicleService.createVehicle(providerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing vehicle's rate, location, seating capacity, or operational status.
     *
     * @param id vehicle ID to update
     * @param providerId caller ID extracted from header X-User-Id
     * @param userRole optional role header for audit tracking
     * @param request vehicle update payload
     * @return 200 OK with updated VehicleResponse
     */
    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long providerId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody VehicleUpdateRequest request) {

        log.info("REST PUT /api/v1/vehicles/{}: providerId={}, role={}", id, providerId, userRole);

        VehicleResponse response = vehicleService.updateVehicle(id, providerId, request);
        return ResponseEntity.ok(response);
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "dailyRentalRate", "createdAt", "manufacturingYear", "make", "model", "seatingCapacity"
    );

    /**
     * Public search endpoint for customer inventory discovery.
     * Supports dynamic filtering by category, location, pricing, fuel type, transmission,
     * minimum seats, with pagination and sorting.
     * Only returns vehicles with AVAILABLE status.
     *
     * @param categoryId optional category ID
     * @param location optional location keyword
     * @param minPrice optional minimum daily rental rate
     * @param maxPrice optional maximum daily rental rate
     * @param fuelType optional fuel type
     * @param transmission optional transmission type
     * @param minSeats optional minimum seating capacity
     * @param page zero-based page number (default: 0)
     * @param size page size (default: 10, max: 50)
     * @param sortBy sort field (default: "dailyRentalRate")
     * @param sortDir sort direction (default: "asc")
     * @return 200 OK with paginated vehicle response
     */
    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<VehicleResponse>> searchVehicles(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "fuelType", required = false) FuelType fuelType,
            @RequestParam(value = "transmission", required = false) TransmissionType transmission,
            @RequestParam(value = "minSeats", required = false) Integer minSeats,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "dailyRentalRate") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir) {

        log.info("REST GET /api/v1/vehicles/search: location={}, minPrice={}, maxPrice={}, page={}, size={}, sortBy={}",
                location, minPrice, maxPrice, page, size, sortBy);

        // Sanitize pagination parameters
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = (size <= 0) ? 10 : Math.min(size, 50);

        // Sanitize sorting field to prevent query injection / invalid property paths
        String sanitizedSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "dailyRentalRate";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(direction, sanitizedSortBy));

        VehicleSearchCriteria criteria = VehicleSearchCriteria.builder()
                .categoryId(categoryId)
                .location(location)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .fuelType(fuelType)
                .transmission(transmission)
                .minSeats(minSeats)
                .build();

        PaginatedResponse<VehicleResponse> response = vehicleService.searchVehicles(criteria, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Publicly accessible query to view single vehicle details by ID.
     *
     * @param id vehicle ID
     * @return 200 OK with VehicleResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable("id") Long id) {
        log.info("REST GET /api/v1/vehicles/{}", id);

        VehicleResponse response = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all vehicles owned and managed by the authenticated provider.
     *
     * @param providerId caller ID extracted from header X-User-Id
     * @param userRole optional role header
     * @return 200 OK with list of VehicleResponse
     */
    @GetMapping("/provider/my-fleet")
    public ResponseEntity<List<VehicleResponse>> getMyFleet(
            @RequestHeader("X-User-Id") Long providerId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        log.info("REST GET /api/v1/vehicles/provider/my-fleet: providerId={}, role={}", providerId, userRole);

        List<VehicleResponse> fleet = vehicleService.getVehiclesByProvider(providerId);
        return ResponseEntity.ok(fleet);
    }

    /**
     * Decommissions or deletes a vehicle owned by the authenticated provider.
     *
     * @param id vehicle ID to delete
     * @param providerId caller ID extracted from header X-User-Id
     * @param userRole optional role header
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long providerId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        log.info("REST DELETE /api/v1/vehicles/{}: providerId={}, role={}", id, providerId, userRole);

        vehicleService.deleteVehicle(id, providerId);
        return ResponseEntity.noContent().build();
    }
}

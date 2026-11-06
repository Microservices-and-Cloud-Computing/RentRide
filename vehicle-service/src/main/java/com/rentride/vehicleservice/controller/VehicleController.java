package com.rentride.vehicleservice.controller;

import com.rentride.vehicleservice.dto.ErrorResponse;
import com.rentride.vehicleservice.dto.PaginatedResponse;
import com.rentride.vehicleservice.dto.VehicleAvailabilityResponse;
import com.rentride.vehicleservice.dto.VehicleCreateRequest;
import com.rentride.vehicleservice.dto.VehicleResponse;
import com.rentride.vehicleservice.dto.VehicleSearchCriteria;
import com.rentride.vehicleservice.dto.VehicleStatusUpdateRequest;
import com.rentride.vehicleservice.dto.VehicleUpdateRequest;
import com.rentride.vehicleservice.entity.FuelType;
import com.rentride.vehicleservice.entity.TransmissionType;
import com.rentride.vehicleservice.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.PatchMapping;
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
 * public vehicle catalog lookups, rental provider fleet management,
 * and internal lifecycle status synchronization.
 *
 * <p>Extracts caller identity context (such as {@code X-User-Id} and {@code X-User-Role})
 * forwarded from the API Gateway authentication filter for provider operations.
 */
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Vehicle Management", description = "Endpoints for vehicle catalog, category discovery, provider fleet operations, and availability status updates")
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
    @Operation(
            summary = "Register a new vehicle",
            description = "Allows rental providers to onboard and register a new vehicle into their active fleet inventory",
            security = { @SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "X-User-Id") }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vehicle registered successfully",
                    content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed for request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate license plate number conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VehicleResponse> createVehicle(
            @Parameter(description = "Provider user ID forwarded by gateway", required = true)
            @RequestHeader("X-User-Id") Long providerId,
            @Parameter(description = "Caller role forwarded by gateway")
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
    @Operation(
            summary = "Update vehicle listing",
            description = "Allows the owning provider to update pricing, location, seating capacity, or operational status of a vehicle",
            security = { @SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "X-User-Id") }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle updated successfully",
                    content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed for request payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller does not own this vehicle",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VehicleResponse> updateVehicle(
            @Parameter(description = "Vehicle unique ID", required = true)
            @PathVariable("id") Long id,
            @Parameter(description = "Provider user ID forwarded by gateway", required = true)
            @RequestHeader("X-User-Id") Long providerId,
            @Parameter(description = "Caller role forwarded by gateway")
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
    @Operation(
            summary = "Search available vehicle inventory",
            description = "Public catalog search allowing customers to filter active vehicles by category, location, price range, fuel type, transmission, and seat count with pagination and sorting"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of available vehicles matching search criteria",
                    content = @Content(schema = @Schema(implementation = PaginatedResponse.class)))
    })
    public ResponseEntity<PaginatedResponse<VehicleResponse>> searchVehicles(
            @Parameter(description = "Filter by category ID")
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @Parameter(description = "Filter by location substring")
            @RequestParam(value = "location", required = false) String location,
            @Parameter(description = "Filter by minimum daily rental rate")
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @Parameter(description = "Filter by maximum daily rental rate")
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @Parameter(description = "Filter by fuel type")
            @RequestParam(value = "fuelType", required = false) FuelType fuelType,
            @Parameter(description = "Filter by transmission type")
            @RequestParam(value = "transmission", required = false) TransmissionType transmission,
            @Parameter(description = "Filter by minimum seating capacity")
            @RequestParam(value = "minSeats", required = false) Integer minSeats,
            @Parameter(description = "Zero-based page index (default 0)")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Number of items per page (default 10, max 50)")
            @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(description = "Sort property (dailyRentalRate, createdAt, manufacturingYear, make, model, seatingCapacity)")
            @RequestParam(value = "sortBy", defaultValue = "dailyRentalRate") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)")
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
    @Operation(
            summary = "Get vehicle by ID",
            description = "Public catalog endpoint to view full specifications and details of an individual vehicle"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VehicleResponse> getVehicleById(
            @Parameter(description = "Vehicle unique ID", required = true)
            @PathVariable("id") Long id) {
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
    @Operation(
            summary = "Get provider fleet",
            description = "Retrieves all vehicles owned and managed by the authenticated provider",
            security = { @SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "X-User-Id") }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of vehicles in the provider's fleet",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = VehicleResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Missing required X-User-Id header",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<VehicleResponse>> getMyFleet(
            @Parameter(description = "Provider user ID forwarded by gateway", required = true)
            @RequestHeader("X-User-Id") Long providerId,
            @Parameter(description = "Caller role forwarded by gateway")
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
    @Operation(
            summary = "Delete vehicle listing",
            description = "Allows the owning provider to remove or decommission a vehicle from their active fleet",
            security = { @SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "X-User-Id") }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vehicle deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Caller does not own this vehicle",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteVehicle(
            @Parameter(description = "Vehicle unique ID", required = true)
            @PathVariable("id") Long id,
            @Parameter(description = "Provider user ID forwarded by gateway", required = true)
            @RequestHeader("X-User-Id") Long providerId,
            @Parameter(description = "Caller role forwarded by gateway")
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        log.info("REST DELETE /api/v1/vehicles/{}: providerId={}, role={}", id, providerId, userRole);

        vehicleService.deleteVehicle(id, providerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates an existing vehicle's operational status.
     * Internal endpoint used by Booking Service to atomically transition vehicle status
     * (AVAILABLE -> RENTED, RENTED -> AVAILABLE, RENTED -> MAINTENANCE, MAINTENANCE -> AVAILABLE)
     * during reservation lifecycle events.
     *
     * @param id vehicle ID
     * @param request status update payload
     * @return 200 OK with updated VehicleResponse
     */
    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Update vehicle operational status",
            description = "Internal service endpoint used to atomically transition vehicle status (AVAILABLE <-> RENTED, RENTED -> MAINTENANCE, MAINTENANCE -> AVAILABLE) during reservation lifecycle events"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle status updated successfully",
                    content = @Content(schema = @Schema(implementation = VehicleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed for status update payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Invalid operational state transition conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VehicleResponse> updateVehicleStatus(
            @Parameter(description = "Vehicle unique ID", required = true)
            @PathVariable("id") Long id,
            @Valid @RequestBody VehicleStatusUpdateRequest request) {

        log.info("REST PATCH /api/v1/vehicles/{}/status: newStatus={}, reason='{}'",
                id, request.getStatus(), request.getReason());

        VehicleResponse response = vehicleService.updateVehicleStatus(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lightweight query endpoint to verify vehicle existence, current status, and rental eligibility.
     *
     * @param id vehicle ID
     * @return 200 OK with VehicleAvailabilityResponse
     */
    @GetMapping("/{id}/availability")
    @Operation(
            summary = "Check vehicle availability",
            description = "Lightweight query endpoint to verify vehicle existence, current status, and rental eligibility before reservation"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle availability status and daily rate",
                    content = @Content(schema = @Schema(implementation = VehicleAvailabilityResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VehicleAvailabilityResponse> checkVehicleAvailability(
            @Parameter(description = "Vehicle unique ID", required = true)
            @PathVariable("id") Long id) {

        log.info("REST GET /api/v1/vehicles/{}/availability", id);

        VehicleAvailabilityResponse response = vehicleService.checkVehicleAvailability(id);
        return ResponseEntity.ok(response);
    }
}

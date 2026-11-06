package com.rentride.vehicleservice.service;

import com.rentride.vehicleservice.dto.PaginatedResponse;
import com.rentride.vehicleservice.dto.VehicleAvailabilityResponse;
import com.rentride.vehicleservice.dto.VehicleCreateRequest;
import com.rentride.vehicleservice.dto.VehicleResponse;
import com.rentride.vehicleservice.dto.VehicleSearchCriteria;
import com.rentride.vehicleservice.dto.VehicleStatusUpdateRequest;
import com.rentride.vehicleservice.dto.VehicleUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface defining vehicle inventory management and rental provider fleet operations.
 */
public interface VehicleService {

    /**
     * Registers a new vehicle under the specified provider.
     *
     * @param providerId ID of the rental provider registering the vehicle
     * @param request vehicle creation attributes
     * @return response payload containing persisted vehicle details
     */
    VehicleResponse createVehicle(Long providerId, VehicleCreateRequest request);

    /**
     * Updates an existing vehicle's rate, location, seating capacity, or operational status.
     * Enforces provider ownership check.
     *
     * @param vehicleId unique ID of the vehicle to update
     * @param providerId ID of the requesting provider
     * @param request updated attributes
     * @return updated vehicle response payload
     */
    VehicleResponse updateVehicle(Long vehicleId, Long providerId, VehicleUpdateRequest request);

    /**
     * Retrieves detailed information for a specific vehicle by ID.
     *
     * @param vehicleId unique vehicle ID
     * @return vehicle response details
     */
    VehicleResponse getVehicleById(Long vehicleId);

    /**
     * Retrieves all vehicles owned by a specific provider.
     *
     * @param providerId unique provider ID
     * @return list of vehicles in the provider's fleet
     */
    List<VehicleResponse> getVehiclesByProvider(Long providerId);

    /**
     * Decommissions or removes a vehicle from the provider's active fleet.
     * Enforces provider ownership check.
     *
     * @param vehicleId unique ID of the vehicle
     * @param providerId ID of the requesting provider
     */
    void deleteVehicle(Long vehicleId, Long providerId);

    /**
     * Executes a dynamic multi-criteria search for vehicles with pagination and sorting.
     * Only returns vehicles with AVAILABLE status.
     *
     * @param criteria dynamic filtering criteria
     * @param pageable pagination and sorting configuration
     * @return paginated response containing matching vehicle records
     */
    PaginatedResponse<VehicleResponse> searchVehicles(VehicleSearchCriteria criteria, Pageable pageable);

    /**
     * Atomically transitions a vehicle's operational status during reservation lifecycle events.
     * Enforces concurrency control and domain state machine validation.
     *
     * @param vehicleId unique ID of the vehicle to update
     * @param request status update payload containing target status and optional reason
     * @return updated vehicle response details
     */
    VehicleResponse updateVehicleStatus(Long vehicleId, VehicleStatusUpdateRequest request);

    /**
     * Performs a lightweight query verifying vehicle existence, status, and reservation availability.
     *
     * @param vehicleId unique vehicle ID
     * @return vehicle availability response payload
     */
    VehicleAvailabilityResponse checkVehicleAvailability(Long vehicleId);
}

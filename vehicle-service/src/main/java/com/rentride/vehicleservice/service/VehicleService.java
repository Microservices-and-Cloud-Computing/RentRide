package com.rentride.vehicleservice.service;

import com.rentride.vehicleservice.dto.VehicleCreateRequest;
import com.rentride.vehicleservice.dto.VehicleResponse;
import com.rentride.vehicleservice.dto.VehicleUpdateRequest;

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
}

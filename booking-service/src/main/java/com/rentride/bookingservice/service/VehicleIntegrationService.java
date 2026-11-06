package com.rentride.bookingservice.service;

import com.rentride.bookingservice.client.VehicleServiceClient;
import com.rentride.bookingservice.dto.external.VehicleAvailabilityDto;
import com.rentride.bookingservice.dto.external.VehicleResponseDto;
import com.rentride.bookingservice.dto.external.VehicleStatusUpdateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service wrapper for {@link VehicleServiceClient} RPC calls.
 *
 * <p>Enriches inter-service communication with logging, parameter validation,
 * and high-level convenience methods for booking lifecycle workflows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleIntegrationService {

    private final VehicleServiceClient vehicleServiceClient;

    /**
     * Retrieves full vehicle details from vehicle-service.
     *
     * @param vehicleId unique ID of the vehicle
     * @return VehicleResponseDto containing full vehicle specifications
     */
    public VehicleResponseDto getVehicleById(Long vehicleId) {
        log.info("RPC: Fetching vehicle specifications for ID: {}", vehicleId);
        VehicleResponseDto response = vehicleServiceClient.getVehicleById(vehicleId);
        log.debug("RPC: Successfully retrieved vehicle ID {}: make={}, model={}, rate={}",
                vehicleId, response.getMake(), response.getModel(), response.getDailyRentalRate());
        return response;
    }

    /**
     * Checks if a vehicle is currently eligible and available for reservation.
     *
     * @param vehicleId unique ID of the vehicle
     * @return VehicleAvailabilityDto with current availability status and daily rate
     */
    public VehicleAvailabilityDto checkAvailability(Long vehicleId) {
        log.info("RPC: Checking availability for vehicle ID: {}", vehicleId);
        VehicleAvailabilityDto availability = vehicleServiceClient.checkAvailability(vehicleId);
        log.debug("RPC: Availability for vehicle ID {}: isAvailable={}, status={}, dailyRate={}",
                vehicleId, availability.isAvailable(), availability.getStatus(), availability.getDailyRentalRate());
        return availability;
    }

    /**
     * Convenience method to verify if a vehicle is available for rental.
     *
     * @param vehicleId unique ID of the vehicle
     * @return {@code true} if vehicle is available; {@code false} otherwise
     */
    public boolean isVehicleAvailable(Long vehicleId) {
        return checkAvailability(vehicleId).isAvailable();
    }

    /**
     * Fetches current daily rental rate of a vehicle.
     *
     * @param vehicleId unique ID of the vehicle
     * @return daily rental rate
     */
    public BigDecimal getDailyRentalRate(Long vehicleId) {
        return checkAvailability(vehicleId).getDailyRentalRate();
    }

    /**
     * Transitions vehicle operational status (e.g., AVAILABLE -> RENTED or RENTED -> AVAILABLE).
     *
     * @param vehicleId unique ID of the vehicle
     * @param targetStatus target operational status (e.g., RENTED, AVAILABLE, MAINTENANCE)
     * @param reason business reason or booking reference triggering the update
     * @return updated VehicleResponseDto
     */
    public VehicleResponseDto updateVehicleStatus(Long vehicleId, String targetStatus, String reason) {
        log.info("RPC: Requesting vehicle status update: vehicleId={}, targetStatus={}, reason='{}'",
                vehicleId, targetStatus, reason);

        VehicleStatusUpdateDto updateRequest = VehicleStatusUpdateDto.builder()
                .status(targetStatus)
                .reason(reason)
                .build();

        VehicleResponseDto response = vehicleServiceClient.updateVehicleStatus(vehicleId, updateRequest);
        log.info("RPC: Vehicle status updated successfully: vehicleId={}, newStatus={}", vehicleId, response.getStatus());
        return response;
    }
}


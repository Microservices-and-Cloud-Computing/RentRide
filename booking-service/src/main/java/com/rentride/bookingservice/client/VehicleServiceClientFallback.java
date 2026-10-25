package com.rentride.bookingservice.client;

import com.rentride.bookingservice.dto.external.VehicleAvailabilityDto;
import com.rentride.bookingservice.dto.external.VehicleResponseDto;
import com.rentride.bookingservice.dto.external.VehicleStatusUpdateDto;
import com.rentride.bookingservice.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for {@link VehicleServiceClient} invoked when vehicle-service
 * is unreachable, timing out, or the circuit breaker trips.
 */
@Slf4j
@Component
public class VehicleServiceClientFallback implements VehicleServiceClient {

    @Override
    public VehicleResponseDto getVehicleById(Long id) {
        log.error("VehicleServiceClient fallback invoked for getVehicleById: vehicleId={}", id);
        throw new ServiceUnavailableException("Vehicle service is temporarily unreachable. Unable to fetch vehicle: " + id);
    }

    @Override
    public VehicleAvailabilityDto checkAvailability(Long id) {
        log.error("VehicleServiceClient fallback invoked for checkAvailability: vehicleId={}", id);
        throw new ServiceUnavailableException("Vehicle service is temporarily unreachable. Unable to check availability for vehicle: " + id);
    }

    @Override
    public VehicleResponseDto updateVehicleStatus(Long id, VehicleStatusUpdateDto request) {
        log.error("VehicleServiceClient fallback invoked for updateVehicleStatus: vehicleId={}, status={}",
                id, request != null ? request.getStatus() : null);
        throw new ServiceUnavailableException("Vehicle service is temporarily unreachable. Unable to update status for vehicle: " + id);
    }
}


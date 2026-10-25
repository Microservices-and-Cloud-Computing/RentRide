package com.rentride.bookingservice.client;

import com.rentride.bookingservice.dto.external.VehicleAvailabilityDto;
import com.rentride.bookingservice.dto.external.VehicleResponseDto;
import com.rentride.bookingservice.dto.external.VehicleStatusUpdateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Declarative Spring Cloud OpenFeign client for RPC communication with vehicle-service.
 *
 * <p>Enables client-side load-balanced invocations via Eureka, resilient fallbacks,
 * and strongly-typed error decoding.
 */
@FeignClient(
        name = "vehicle-service",
        url = "${vehicle-service.url:}",
        configuration = FeignClientConfig.class,
        fallback = VehicleServiceClientFallback.class
)
public interface VehicleServiceClient {

    /**
     * Retrieves complete vehicle specifications by ID.
     *
     * @param id vehicle ID
     * @return vehicle response DTO
     */
    @GetMapping("/api/v1/vehicles/{id}")
    VehicleResponseDto getVehicleById(@PathVariable("id") Long id);

    /**
     * Checks current availability status and daily rate of a vehicle.
     *
     * @param id vehicle ID
     * @return vehicle availability DTO
     */
    @GetMapping("/api/v1/vehicles/{id}/availability")
    VehicleAvailabilityDto checkAvailability(@PathVariable("id") Long id);

    /**
     * Atomically transitions vehicle operational status during booking lifecycle events.
     *
     * @param id vehicle ID
     * @param request target status update payload
     * @return updated vehicle response DTO
     */
    @PatchMapping("/api/v1/vehicles/{id}/status")
    VehicleResponseDto updateVehicleStatus(
            @PathVariable("id") Long id,
            @RequestBody VehicleStatusUpdateDto request
    );
}


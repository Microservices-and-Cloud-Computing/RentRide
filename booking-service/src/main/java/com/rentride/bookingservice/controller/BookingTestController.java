package com.rentride.bookingservice.controller;

import com.rentride.bookingservice.dto.external.VehicleAvailabilityDto;
import com.rentride.bookingservice.dto.external.VehicleResponseDto;
import com.rentride.bookingservice.dto.external.VehicleStatusUpdateDto;
import com.rentride.bookingservice.service.VehicleIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Diagnostic controller for verifying inter-service OpenFeign RPC communication
 * between Booking-Service and Vehicle-Service.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bookings/test")
@RequiredArgsConstructor
public class BookingTestController {

    private final VehicleIntegrationService vehicleIntegrationService;

    /**
     * Cross-service integration test endpoint: retrieves vehicle data via OpenFeign RPC.
     *
     * @param id vehicle ID
     * @return 200 OK with VehicleResponseDto
     */
    @GetMapping("/vehicle/{id}")
    public ResponseEntity<VehicleResponseDto> testVehicleRpc(@PathVariable("id") Long id) {
        log.info("REST GET /api/v1/bookings/test/vehicle/{}: Triggering Feign RPC to vehicle-service", id);
        VehicleResponseDto vehicle = vehicleIntegrationService.getVehicleById(id);
        return ResponseEntity.ok(vehicle);
    }

    /**
     * Cross-service integration test endpoint: checks vehicle availability via OpenFeign RPC.
     *
     * @param id vehicle ID
     * @return 200 OK with VehicleAvailabilityDto
     */
    @GetMapping("/vehicle/{id}/availability")
    public ResponseEntity<VehicleAvailabilityDto> testVehicleAvailabilityRpc(@PathVariable("id") Long id) {
        log.info("REST GET /api/v1/bookings/test/vehicle/{}/availability: Triggering Feign RPC to vehicle-service", id);
        VehicleAvailabilityDto availability = vehicleIntegrationService.checkAvailability(id);
        return ResponseEntity.ok(availability);
    }

    /**
     * Cross-service integration test endpoint: updates vehicle status via OpenFeign RPC.
     *
     * @param id vehicle ID
     * @param request status update request
     * @return 200 OK with updated VehicleResponseDto
     */
    @PatchMapping("/vehicle/{id}/status")
    public ResponseEntity<VehicleResponseDto> testVehicleStatusUpdateRpc(
            @PathVariable("id") Long id,
            @RequestBody VehicleStatusUpdateDto request) {
        log.info("REST PATCH /api/v1/bookings/test/vehicle/{}/status: Triggering Feign RPC with status={}", id, request.getStatus());
        VehicleResponseDto updated = vehicleIntegrationService.updateVehicleStatus(id, request.getStatus(), request.getReason());
        return ResponseEntity.ok(updated);
    }
}



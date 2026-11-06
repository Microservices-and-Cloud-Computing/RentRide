package com.rentride.vehicleservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health and verification controller for the vehicle-service microservice.
 * Exposes lightweight diagnostic endpoints.
 */
@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleStatusController {

    @Value("${server.port:8082}")
    private int port;

    @Value("${spring.application.name:vehicle-service}")
    private String service;

    /**
     * Diagnostic status endpoint verifying vehicle-service liveness and identity.
     *
     * @return lightweight status JSON object
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> statusResponse = new LinkedHashMap<>();
        statusResponse.put("status", "UP");
        statusResponse.put("service", service);
        statusResponse.put("port", port);

        return ResponseEntity.ok(statusResponse);
    }
}

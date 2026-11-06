package com.rentride.notificationservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Diagnostic status and health controller for the notification-service microservice.
 *
 * <p>Exposes a lightweight liveness endpoint that returns service identity
 * and port metadata. Used for quick sanity-checks, Eureka registration
 * verification, and API Gateway route validation.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationStatusController {

    @Value("${server.port:8085}")
    private int port;

    @Value("${spring.application.name:notification-service}")
    private String service;

    /**
     * Diagnostic status endpoint verifying notification-service liveness and identity.
     *
     * @return lightweight JSON status object: {@code {status, service, port}}
     */
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> statusResponse = new LinkedHashMap<>();
        statusResponse.put("status", "UP");
        statusResponse.put("service", service);
        statusResponse.put("port", port);

        return ResponseEntity.ok(statusResponse);
    }
}

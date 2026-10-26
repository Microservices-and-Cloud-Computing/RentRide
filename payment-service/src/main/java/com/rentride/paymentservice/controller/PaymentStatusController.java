package com.rentride.paymentservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Diagnostic status and health controller for the payment-service microservice.
 * Exposes lightweight liveness, service identification, and port metadata.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentStatusController {

    @Value("${server.port:8084}")
    private int port;

    @Value("${spring.application.name:payment-service}")
    private String service;

    /**
     * Diagnostic status endpoint verifying payment-service liveness and identity.
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


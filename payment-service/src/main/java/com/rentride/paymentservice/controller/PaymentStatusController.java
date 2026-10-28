package com.rentride.paymentservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
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
@Tag(
        name = "Service Diagnostics",
        description = "Lightweight liveness and service-identification endpoints for the payment-service"
)
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
    @Operation(
            summary = "Payment-service liveness status",
            description = "Returns a lightweight JSON payload confirming the payment-service is running, " +
                          "along with the active service name and bound port number. " +
                          "Intended for quick sanity-checks and integration testing."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Service is running and healthy",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    name = "Healthy status",
                                    value = """
                                            {
                                              "status": "UP",
                                              "service": "payment-service",
                                              "port": 8084
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> statusResponse = new LinkedHashMap<>();
        statusResponse.put("status", "UP");
        statusResponse.put("service", service);
        statusResponse.put("port", port);

        return ResponseEntity.ok(statusResponse);
    }
}

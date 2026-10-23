package com.rentride.userservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight liveness / sanity controller for the User Microservice.
 *
 * <p>Exposes a single {@code GET /api/v1/users/status} endpoint that returns a
 * human-readable status payload.  This endpoint is intentionally kept unauthenticated
 * so that:
 * <ul>
 *   <li>The API Gateway health-checker can confirm the service is reachable.</li>
 *   <li>The Kubernetes / Docker liveness probe can ping without a token.</li>
 *   <li>QA teams can quickly verify end-to-end gateway routing in each environment.</li>
 * </ul>
 *
 * <p><b>Note:</b> For deeper health metrics (DB pool, disk, memory) use the
 * Actuator endpoint at {@code /actuator/health}.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserStatusController {

    /**
     * Returns a simple status map confirming the service is alive and healthy.
     *
     * <p>Example response:
     * <pre>{@code
     * {
     *   "status"    : "UP",
     *   "service"   : "user-service",
     *   "timestamp" : "2026-10-22T00:00:00Z"
     * }
     * }</pre>
     *
     * @return {@code 200 OK} with a {@link Map} payload serialised as JSON
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "user-service");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }
}

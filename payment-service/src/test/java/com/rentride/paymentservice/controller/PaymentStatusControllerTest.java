package com.rentride.paymentservice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for PaymentStatusController diagnostic liveness endpoint.
 */
@WebMvcTest(PaymentStatusController.class)
class PaymentStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/payments/status: Should return 200 OK with service name, status, and port")
    void testGetStatus_Returns200WithServiceMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/payments/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("payment-service"))
                .andExpect(jsonPath("$.port").value(8084));
    }
}


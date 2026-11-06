package com.rentride.notificationservice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit / slice test for {@link NotificationStatusController}.
 *
 * <p>Uses {@link WebMvcTest} to load only the web layer — no Spring Cloud
 * or mail beans are required for this test.
 */
@WebMvcTest(NotificationStatusController.class)
class NotificationStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/notifications/status → 200 OK with correct JSON payload")
    void getStatus_returns200WithExpectedPayload() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("notification-service"))
                .andExpect(jsonPath("$.port").isNumber());
    }
}

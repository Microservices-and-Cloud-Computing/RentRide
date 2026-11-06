package com.rentride.paymentservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Context load verification test for PaymentServiceApplication.
 */
@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceApplicationTests {

    @Test
    @DisplayName("Context Loads: Verifies complete Payment Microservice Spring context bootstraps cleanly")
    void contextLoads() {
    }
}


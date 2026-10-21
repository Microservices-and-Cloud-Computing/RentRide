package com.rentride.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test that verifies the Spring application context loads successfully.
 *
 * <p>The {@code @TestPropertySource} override disables the Eureka client and
 * replaces the real PostgreSQL datasource with an in-memory H2 datasource so the
 * context can load without any external infrastructure (DB / Eureka) during CI.
 *
 * <p>A full integration test (with Testcontainers) will be added in a later commit.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // ── Disable Eureka registration during tests ──────────────────────
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        // ── Use in-memory H2 instead of PostgreSQL ────────────────────────
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
        // Passes if the Spring context initializes without throwing an exception.
    }
}

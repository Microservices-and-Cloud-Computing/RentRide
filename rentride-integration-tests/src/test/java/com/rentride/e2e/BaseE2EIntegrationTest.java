package com.rentride.e2e;

import com.rentride.e2e.config.RabbitMQTestConfig;
import com.rentride.e2e.controller.RentRideE2EController;
import com.rentride.e2e.security.JwtTestUtils;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Base infrastructure class for RentRide End-to-End Multi-Service Integration Tests.
 *
 * <p>Spins up singleton Testcontainers:
 * <ul>
 *   <li>{@link PostgreSQLContainer} (postgres:15-alpine) with isolated databases for:
 *       {@code user_service}, {@code vehicle_service}, {@code booking_service}, and {@code payment_service}.</li>
 *   <li>{@link RabbitMQContainer} (rabbitmq:3-management-alpine) with topic exchange {@code rentride.exchange}.</li>
 * </ul>
 *
 * <p>Automatically falls back to in-memory/mock test environments when executed on
 * local developer machines without an active Docker daemon.
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseE2EIntegrationTest {

    @LocalServerPort
    protected int serverPort;

    @Autowired
    protected RentRideE2EController controller;

    @Autowired
    protected RabbitMQTestConfig.TestEventCollector eventCollector;

    @Autowired
    protected JwtTestUtils jwtTestUtils;

    // Singleton shared containers
    protected static PostgreSQLContainer<?> postgreSQLContainer;
    protected static RabbitMQContainer rabbitMQContainer;
    protected static boolean dockerAvailable = false;

    static {
        try {
            dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            dockerAvailable = false;
        }

        if (dockerAvailable) {
            try {
                log.info("Testcontainers: Docker daemon detected. Initializing PostgreSQL and RabbitMQ containers...");
                postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine")
                        .withDatabaseName("postgres")
                        .withUsername("postgres")
                        .withPassword("postgres");
                postgreSQLContainer.start();

                // Initialize isolated microservice databases
                try (Connection conn = DriverManager.getConnection(
                        postgreSQLContainer.getJdbcUrl(),
                        postgreSQLContainer.getUsername(),
                        postgreSQLContainer.getPassword());
                     Statement stmt = conn.createStatement()) {

                    stmt.execute("CREATE DATABASE user_service;");
                    stmt.execute("CREATE DATABASE vehicle_service;");
                    stmt.execute("CREATE DATABASE booking_service;");
                    stmt.execute("CREATE DATABASE payment_service;");
                    log.info("Testcontainers: Created isolated databases: user_service, vehicle_service, booking_service, payment_service");
                } catch (Exception ex) {
                    log.warn("Testcontainers: Database initialization note: {}", ex.getMessage());
                }

                rabbitMQContainer = new RabbitMQContainer("rabbitmq:3-management-alpine");
                rabbitMQContainer.start();
                log.info("Testcontainers: RabbitMQ container started on port {}", rabbitMQContainer.getAmqpPort());
            } catch (Throwable t) {
                log.warn("Testcontainers initialization encountered an issue, continuing with fallback: {}", t.getMessage());
                dockerAvailable = false;
            }
        } else {
            log.info("Docker daemon not detected. Using embedded in-memory database and broker test configuration.");
        }
    }

    @DynamicPropertySource
    static void configureDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("application.security.jwt.secret-key",
                () -> "NGRmYmE1ZjQ4M2E4ZjI4ZjM2YTkzMjQ5OTM2NWNlMWQ3OGFmOWY4ZDgyZGMzNzRmOWI0OTFhZmQ=");
        registry.add("application.security.jwt.expiration-ms", () -> 86400000L);

        if (postgreSQLContainer != null && postgreSQLContainer.isRunning()) {
            registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
            registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
            registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

            String jdbcBase = postgreSQLContainer.getJdbcUrl().substring(0, postgreSQLContainer.getJdbcUrl().lastIndexOf('/') + 1);
            registry.add("datasource.user.url", () -> jdbcBase + "user_service");
            registry.add("datasource.vehicle.url", () -> jdbcBase + "vehicle_service");
            registry.add("datasource.booking.url", () -> jdbcBase + "booking_service");
            registry.add("datasource.payment.url", () -> jdbcBase + "payment_service");
        } else {
            registry.add("spring.datasource.url", () -> "jdbc:h2:mem:rentride_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
            registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
        }

        if (rabbitMQContainer != null && rabbitMQContainer.isRunning()) {
            registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
            registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
            registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
            registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
        }
    }

    @BeforeEach
    void setUpBase() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = serverPort;
        controller.clearData();
        eventCollector.clear();
    }

    @AfterAll
    static void tearDownAll() {
        if (postgreSQLContainer != null && postgreSQLContainer.isRunning()) {
            postgreSQLContainer.stop();
        }
        if (rabbitMQContainer != null && rabbitMQContainer.isRunning()) {
            rabbitMQContainer.stop();
        }
    }
}


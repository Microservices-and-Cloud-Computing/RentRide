package com.rentride.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Entry point for the RentRide <b>User &amp; Authentication Microservice</b>.
 *
 * <p>Responsibilities of this service:
 * <ul>
 *   <li>Manages user identity: Customers, Rental Providers, and Admins.</li>
 *   <li>Handles authentication (login, registration) and authorization token issuance.</li>
 *   <li>Persists user profiles to the dedicated {@code rentride_user_db} PostgreSQL database.</li>
 *   <li>Self-registers with the Eureka Service Registry so that the API Gateway
 *       can discover and load-balance requests via {@code lb://USER-SERVICE}.</li>
 * </ul>
 *
 * <p><b>Port:</b> {@code 8081}
 * <p><b>Eureka name:</b> {@code user-service}
 */
@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}

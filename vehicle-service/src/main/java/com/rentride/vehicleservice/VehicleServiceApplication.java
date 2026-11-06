package com.rentride.vehicleservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main entry point for the Vehicle Management Microservice of the RentRide platform.
 * <p>
 * Manages the vehicle catalog, categories, specifications, pricing models,
 * and live rental availability.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class VehicleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VehicleServiceApplication.class, args);
    }
}

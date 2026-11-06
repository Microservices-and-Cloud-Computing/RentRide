package com.rentride.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application entry point for the RentRide Notification Microservice.
 *
 * <p>Responsible for asynchronous customer notifications including transactional
 * emails (booking confirmations, pickup alerts, return receipts) and messaging logs.
 * Registers dynamically with the Eureka Service Registry for load-balanced discovery.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}

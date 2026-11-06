package com.rentride.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main application entry point for the RentRide Payment Microservice.
 *
 * <p>Handles financial transactions, simulated payment gateways, receipt tracking,
 * and booking status reconciliation. Registers dynamically with Eureka Service Registry.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}


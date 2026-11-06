package com.rentride.vehicleservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 / Swagger configuration for the RentRide Vehicle & Inventory Management Microservice.
 * Configures API metadata, direct/gateway server environments, and security schemes for provider fleet operations.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";
    public static final String USER_ID_HEADER = "X-User-Id";

    @Bean
    public OpenAPI vehicleServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RentRide Vehicle & Inventory Service API")
                        .description("Microservice managing vehicle inventory, categories, real-time availability, and provider fleet operations for the RentRide platform")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("RentRide Architecture Team")
                                .email("support@rentride.com")
                                .url("https://rentride.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8082")
                                .description("Direct Local Service"),
                        new Server()
                                .url("http://localhost:8080/api/v1/vehicles")
                                .description("API Gateway URL")
                ))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .description("Enter JWT Bearer token to access protected fleet endpoints"))
                        .addSecuritySchemes(USER_ID_HEADER, new SecurityScheme()
                                .name(USER_ID_HEADER)
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Gateway forwarded provider user ID header")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH).addList(USER_ID_HEADER));
    }
}


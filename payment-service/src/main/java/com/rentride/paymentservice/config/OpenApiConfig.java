package com.rentride.paymentservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 / Swagger UI configuration for the payment-service.
 *
 * <p>Registers API metadata, server URLs, and the {@code X-User-Id} header
 * security scheme that mirrors the propagation performed by the API Gateway.</p>
 *
 * <p>Swagger UI is available at: {@code http://localhost:8084/swagger-ui/index.html}</p>
 * <p>Raw OpenAPI JSON:          {@code http://localhost:8084/v3/api-docs}</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Builds the root {@link OpenAPI} descriptor consumed by springdoc to
     * generate both the interactive Swagger UI and the raw {@code /v3/api-docs}
     * JSON specification.
     *
     * @return fully-populated {@link OpenAPI} object
     */
    @Bean
    public OpenAPI paymentServiceOpenAPI() {

        // ── Info block ────────────────────────────────────────────────────────
        Info info = new Info()
                .title("RentRide Payment & Transaction Processing Service API")
                .version("1.0.0")
                .description(
                        "Microservice handling online reservation checkout, simulated card payment " +
                        "gateway processing, transaction verification, and booking confirmation callbacks.\n\n" +
                        "**Test card numbers (simulated gateway):**\n" +
                        "| Card Number        | Outcome     |\n" +
                        "|--------------------|-------------|\n" +
                        "| 4111111111114242   | Approved    |\n" +
                        "| 4111111111110000   | Declined    |\n\n" +
                        "All monetary amounts default to **LKR**. " +
                        "Set the `X-User-Id` header to the authenticated customer ID before executing endpoints.")
                .contact(new Contact()
                        .name("RentRide Platform Team")
                        .email("platform@rentride.com"));

        // ── Server entries ────────────────────────────────────────────────────
        Server directServer = new Server()
                .url("http://localhost:8084")
                .description("Direct payment-service (port 8084)");

        Server gatewayServer = new Server()
                .url("http://localhost:8080/api/v1/payments")
                .description("API Gateway route (port 8080)");

        // ── Security — X-User-Id header scheme ────────────────────────────────
        final String X_USER_ID_SCHEME = "X-User-Id";

        SecurityScheme xUserIdScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-User-Id")
                .description("Authenticated customer ID propagated by the API Gateway. " +
                             "Supply any numeric user ID (e.g. 1) when calling endpoints directly.");

        // ── Bearer JWT scheme (future SSO integration) ────────────────────────
        final String BEARER_SCHEME = "bearerAuth";

        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT bearer token (reserved for future SSO/OAuth2 integration).");

        return new OpenAPI()
                .info(info)
                .servers(List.of(directServer, gatewayServer))
                .addSecurityItem(new SecurityRequirement().addList(X_USER_ID_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(X_USER_ID_SCHEME, xUserIdScheme)
                        .addSecuritySchemes(BEARER_SCHEME, bearerScheme));
    }
}

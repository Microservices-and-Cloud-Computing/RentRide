package com.rentride.vehicleservice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract and integration verification tests for OpenAPI 3 / Swagger documentation assets
 * in the vehicle-service microservice.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /v3/api-docs: Returns 200 OK with valid OpenAPI 3 schema specification")
    void testOpenApiJsonSpecification_Success() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi", startsWith("3.")))
                .andExpect(jsonPath("$.info.title", is("RentRide Vehicle & Inventory Service API")))
                .andExpect(jsonPath("$.info.version", is("1.0.0")))
                .andExpect(jsonPath("$.info.description", notNullValue()))
                .andExpect(jsonPath("$.servers[0].url", is("http://localhost:8082")))
                .andExpect(jsonPath("$.servers[1].url", is("http://localhost:8080/api/v1/vehicles")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type", is("http")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme", is("bearer")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat", is("JWT")))
                .andExpect(jsonPath("$.components.securitySchemes['X-User-Id'].type", is("apiKey")))
                .andExpect(jsonPath("$.paths['/api/v1/vehicles']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/vehicles/{id}']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/vehicles/search']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/vehicles/{id}/status']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/vehicles/{id}/availability']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/vehicles/provider/my-fleet']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/categories']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/v1/categories/{id}']", notNullValue()));
    }

    @Test
    @DisplayName("GET /swagger-ui.html: Accessible and redirects to Swagger UI index")
    void testSwaggerUiHtml_Accessible() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("GET /swagger-ui/index.html: Accessible and returns 200 OK")
    void testSwaggerUiIndexHtml_Accessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}


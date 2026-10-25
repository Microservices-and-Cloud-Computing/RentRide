package com.rentride.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RouterValidatorTest {

    private RouterValidator routerValidator;

    @BeforeEach
    void setUp() {
        routerValidator = new RouterValidator();
    }

    @Test
    @DisplayName("isSecured: Public auth registration and login endpoints should bypass authentication")
    void testOpenAuthEndpoints_AreNotSecured() {
        assertThat(routerValidator.isSecured(MockServerHttpRequest.post("/api/v1/auth/register").build())).isFalse();
        assertThat(routerValidator.isSecured(MockServerHttpRequest.post("/api/v1/auth/register/customer").build())).isFalse();
        assertThat(routerValidator.isSecured(MockServerHttpRequest.post("/api/v1/auth/register/provider").build())).isFalse();
        assertThat(routerValidator.isSecured(MockServerHttpRequest.post("/api/v1/auth/login").build())).isFalse();
        assertThat(routerValidator.isSecured(MockServerHttpRequest.post("/api/v1/auth/refresh").build())).isFalse();
    }

    @Test
    @DisplayName("isSecured: Actuator, Swagger, and Eureka endpoints should bypass authentication")
    void testPublicDocumentationAndHealthEndpoints_AreNotSecured() {
        assertThat(routerValidator.isSecured(MockServerHttpRequest.get("/actuator/health").build())).isFalse();
        assertThat(routerValidator.isSecured(MockServerHttpRequest.get("/api/v1/vehicles/status").build())).isFalse();
        assertThat(routerValidator.isSecured(MockServerHttpRequest.get("/api/v1/bookings/status").build())).isFalse();
        assertThat(routerValidator.isSecured(MockServerHttpRequest.get("/v3/api-docs").build())).isFalse();
        assertThat(routerValidator.isSecured(MockServerHttpRequest.get("/swagger-ui/index.html").build())).isFalse();
        assertThat(routerValidator.isSecured(MockServerHttpRequest.get("/swagger-ui.html").build())).isFalse();
        assertThat(routerValidator.isSecured(MockServerHttpRequest.get("/eureka/apps").build())).isFalse();
    }

    @Test
    @DisplayName("isSecured: Protected user and business routes must require authentication")
    void testProtectedRoutes_AreSecured() {
        assertThat(routerValidator.isSecured(MockServerHttpRequest.get("/api/v1/users/me").build())).isTrue();
        assertThat(routerValidator.isSecured(MockServerHttpRequest.get("/api/v1/users").build())).isTrue();
        assertThat(routerValidator.isSecured(MockServerHttpRequest.get("/api/v1/vehicles").build())).isTrue();
        assertThat(routerValidator.isSecured(MockServerHttpRequest.post("/api/v1/bookings").build())).isTrue();
        assertThat(routerValidator.isSecured(MockServerHttpRequest.post("/api/v1/payments").build())).isTrue();
        assertThat(routerValidator.isSecured(MockServerHttpRequest.get("/api/v1/notifications").build())).isTrue();
    }
}

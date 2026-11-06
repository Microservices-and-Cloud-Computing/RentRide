package com.rentride.gateway.filter;

import com.rentride.gateway.util.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationFilterTest {

    private TestRouterValidator routerValidator;
    private TestJwtUtils jwtUtils;
    private TestFilterChain filterChain;
    private AuthenticationFilter authenticationFilter;

    @BeforeEach
    void setUp() {
        routerValidator = new TestRouterValidator();
        jwtUtils = new TestJwtUtils();
        filterChain = new TestFilterChain();
        authenticationFilter = new AuthenticationFilter(routerValidator, jwtUtils);
    }

    @Test
    @DisplayName("apply: Whitelisted route bypasses authentication and proceeds down filter chain")
    void testWhitelistedRoute_BypassesAuthentication() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        routerValidator.secured = false;

        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        assertThat(filterChain.invoked).isTrue();
        assertThat(jwtUtils.validateCalled).isFalse();
    }

    @Test
    @DisplayName("apply: Secured route missing Authorization header returns HTTP 401 Unauthorized")
    void testSecuredRoute_MissingAuthHeader_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        routerValidator.secured = true;

        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        MockServerWebExchange mockExchange = (MockServerWebExchange) exchange;
        DataBuffer buffer = mockExchange.getResponse().getBody().blockFirst();
        assertThat(buffer).isNotNull();
        String responseBody = buffer.toString(StandardCharsets.UTF_8);
        assertThat(responseBody).contains("\"status\":401");
        assertThat(responseBody).contains("\"error\":\"Unauthorized\"");
        assertThat(responseBody).contains("Missing or invalid authorization token");
        assertThat(filterChain.invoked).isFalse();
    }

    @Test
    @DisplayName("apply: Secured route with non-Bearer header returns HTTP 401 Unauthorized")
    void testSecuredRoute_NonBearerHeader_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        routerValidator.secured = true;

        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(filterChain.invoked).isFalse();
    }

    @Test
    @DisplayName("apply: Secured route with invalid JWT signature or expired token returns HTTP 401 Unauthorized")
    void testSecuredRoute_InvalidJwt_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        routerValidator.secured = true;
        jwtUtils.valid = false;

        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(filterChain.invoked).isFalse();
    }

    @Test
    @DisplayName("apply: Secured route with valid JWT injects X-User headers and forwards downstream")
    void testSecuredRoute_ValidJwt_InjectsHeadersAndForwards() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid.jwt.token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Claims testClaims = Jwts.claims()
                .subject("valid.user@rentride.com")
                .add("userId", 99L)
                .add("role", "ROLE_CUSTOMER")
                .build();

        routerValidator.secured = true;
        jwtUtils.valid = true;
        jwtUtils.claims = testClaims;

        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        assertThat(filterChain.invoked).isTrue();
        assertThat(filterChain.capturedExchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("99");
        assertThat(filterChain.capturedExchange.getRequest().getHeaders().getFirst("X-User-Email")).isEqualTo("valid.user@rentride.com");
        assertThat(filterChain.capturedExchange.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("ROLE_CUSTOMER");
    }

    // --- Lightweight Test Doubles (Zero Mockito Overhead / Java 21 dynamic attach safe) ---

    private static class TestRouterValidator extends RouterValidator {
        boolean secured = false;

        @Override
        public boolean isSecured(ServerHttpRequest request) {
            return secured;
        }
    }

    private static class TestJwtUtils extends JwtUtils {
        boolean valid = false;
        boolean validateCalled = false;
        Claims claims;

        @Override
        public boolean validateToken(String token) {
            this.validateCalled = true;
            return valid;
        }

        @Override
        public Claims getClaims(String token) {
            return claims;
        }
    }

    private static class TestFilterChain implements GatewayFilterChain {
        boolean invoked = false;
        ServerWebExchange capturedExchange;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            this.invoked = true;
            this.capturedExchange = exchange;
            return Mono.empty();
        }
    }
}

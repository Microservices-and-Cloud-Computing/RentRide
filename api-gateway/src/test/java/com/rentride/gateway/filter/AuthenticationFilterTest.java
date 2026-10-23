package com.rentride.gateway.filter;

import com.rentride.gateway.util.JwtUtils;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private RouterValidator routerValidator;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private GatewayFilterChain filterChain;

    private AuthenticationFilter authenticationFilter;

    @BeforeEach
    void setUp() {
        authenticationFilter = new AuthenticationFilter(routerValidator, jwtUtils);
    }

    @Test
    @DisplayName("apply: Whitelisted route bypasses authentication and proceeds down filter chain")
    void testWhitelistedRoute_BypassesAuthentication() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(routerValidator.isSecured(any())).thenReturn(false);
        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        verify(filterChain).filter(exchange);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    @DisplayName("apply: Secured route missing Authorization header returns HTTP 401 Unauthorized")
    void testSecuredRoute_MissingAuthHeader_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(routerValidator.isSecured(any())).thenReturn(true);

        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        MockServerWebExchange mockExchange = (MockServerWebExchange) exchange;
        String responseBody = mockExchange.getResponse().getBodyAsString().block();
        assertThat(responseBody).contains("\"status\":401");
        assertThat(responseBody).contains("\"error\":\"Unauthorized\"");
        assertThat(responseBody).contains("Missing or invalid authorization token");
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("apply: Secured route with non-Bearer header returns HTTP 401 Unauthorized")
    void testSecuredRoute_NonBearerHeader_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(routerValidator.isSecured(any())).thenReturn(true);

        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("apply: Secured route with invalid JWT signature or expired token returns HTTP 401 Unauthorized")
    void testSecuredRoute_InvalidJwt_Returns401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(routerValidator.isSecured(any())).thenReturn(true);
        when(jwtUtils.validateToken("invalid.jwt.token")).thenReturn(false);

        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(filterChain, never()).filter(any());
    }

    @Test
    @DisplayName("apply: Secured route with valid JWT injects X-User headers and forwards downstream")
    void testSecuredRoute_ValidJwt_InjectsHeadersAndForwards() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid.jwt.token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Claims mockClaims = mock(Claims.class);
        when(mockClaims.get("userId")).thenReturn(99L);
        when(mockClaims.getSubject()).thenReturn("valid.user@rentride.com");
        when(mockClaims.get("role")).thenReturn("ROLE_CUSTOMER");

        when(routerValidator.isSecured(any())).thenReturn(true);
        when(jwtUtils.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtUtils.getClaims("valid.jwt.token")).thenReturn(mockClaims);
        when(filterChain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            ServerWebExchange mutatedExchange = invocation.getArgument(0);
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("99");
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Email")).isEqualTo("valid.user@rentride.com");
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("ROLE_CUSTOMER");
            return Mono.empty();
        });

        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, filterChain);

        StepVerifier.create(result).verifyComplete();
        verify(filterChain).filter(any(ServerWebExchange.class));
    }
}

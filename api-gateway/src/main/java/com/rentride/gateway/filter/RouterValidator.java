package com.rentride.gateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * Route validator determining whether an incoming request URI requires
 * JWT authentication or is whitelisted for public/open access.
 */
@Component
public class RouterValidator {

    public static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/register/**",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/vehicles/status",
            "/eureka/**",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Predicate verifying if the incoming HTTP request targets a secured path.
     *
     * @param request the reactive HTTP request
     * @return {@code true} if authentication is required; {@code false} if whitelisted
     */
    public boolean isSecured(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        return OPEN_API_ENDPOINTS.stream()
                .noneMatch(pattern -> pathMatcher.match(pattern, path));
    }
}

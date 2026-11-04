package com.rentride.e2e.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Edge security filter simulating the API Gateway AuthenticationFilter and RouterValidator.
 * Validates JWT tokens, enforces role-based endpoint permissions, and injects identity headers
 * (X-User-Id, X-User-Email, X-User-Role) for downstream microservice consumption.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class GatewaySecurityFilter implements Filter {

    private final JwtTestUtils jwtTestUtils;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/register/**",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/categories",
            "/api/v1/categories/**",
            "/api/v1/vehicles/status",
            "/api/v1/vehicles/search",
            "/api/v1/vehicles/{id:[0-9]+}",
            "/api/v1/vehicles/{id:[0-9]+}/availability",
            "/api/v1/bookings/status",
            "/api/v1/payments/status",
            "/api/v1/payments/verify/**",
            "/actuator/**",
            "/eureka/**",
            "/api/v2/messages"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        log.debug("GatewaySecurityFilter: {} {}", method, path);

        boolean isSecured = isSecuredPath(path, method);

        if (!isSecured) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("GatewaySecurityFilter: Missing or invalid Authorization header for path {}", path);
            sendJsonError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid authorization token");
            return;
        }

        String token = authHeader.substring(7).trim();
        if (!jwtTestUtils.validateToken(token)) {
            log.warn("GatewaySecurityFilter: Expired or unparseable JWT token for path {}", path);
            sendJsonError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid authorization token");
            return;
        }

        try {
            Claims claims = jwtTestUtils.getClaims(token);
            String userId = String.valueOf(claims.get("userId"));
            String email = claims.getSubject();
            String role = String.valueOf(claims.get("role"));

            log.debug("GatewaySecurityFilter: Token validated. userId={}, email={}, role={}", userId, email, role);

            // Role enforcement: provider fleet & lifecycle operations require PROVIDER or ADMIN
            if (path.startsWith("/api/v1/vehicles/provider") || (path.equals("/api/v1/vehicles") && "POST".equalsIgnoreCase(method))
                    || path.contains("/pickup") || path.contains("/return")) {
                if (!"PROVIDER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
                    log.warn("GatewaySecurityFilter: Access denied for role {} on path {}", role, path);
                    sendJsonError(httpResponse, HttpServletResponse.SC_FORBIDDEN, "Access denied: insufficient privileges");
                    return;
                }
            }

            HeaderMapRequestWrapper wrappedRequest = new HeaderMapRequestWrapper(httpRequest);
            wrappedRequest.addHeader("X-User-Id", userId);
            wrappedRequest.addHeader("X-User-Email", email);
            wrappedRequest.addHeader("X-User-Role", role);

            chain.doFilter(wrappedRequest, response);
        } catch (Exception e) {
            log.error("GatewaySecurityFilter: Error processing token: {}", e.getMessage());
            sendJsonError(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid authorization token");
        }
    }

    private boolean isSecuredPath(String path, String method) {
        for (String pattern : OPEN_API_ENDPOINTS) {
            if (pathMatcher.match(pattern, path)) {
                return false;
            }
        }
        return true;
    }

    private void sendJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String json = String.format("{\"status\":%d,\"message\":\"%s\",\"timestamp\":\"%s\"}",
                status, message, java.time.LocalDateTime.now());
        response.getWriter().write(json);
    }

    private static class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> customHeaders = new HashMap<>();

        public HeaderMapRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        public void addHeader(String name, String value) {
            customHeaders.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String headerValue = customHeaders.get(name);
            if (headerValue != null) {
                return headerValue;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> set = new HashSet<>(customHeaders.keySet());
            Enumeration<String> e = ((HttpServletRequest) getRequest()).getHeaderNames();
            while (e.hasMoreElements()) {
                set.add(e.nextElement());
            }
            return Collections.enumeration(set);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String value = customHeaders.get(name);
            if (value != null) {
                return Collections.enumeration(Collections.singletonList(value));
            }
            return super.getHeaders(name);
        }
    }
}


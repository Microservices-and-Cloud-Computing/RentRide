package com.rentride.gateway.filter;

import com.rentride.gateway.util.JwtUtils;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Spring Cloud Gateway reactive filter factory providing edge-level JWT validation,
 * token verification, and downstream identity header propagation.
 */
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final RouterValidator routerValidator;
    private final JwtUtils jwtUtils;

    public AuthenticationFilter(RouterValidator routerValidator, JwtUtils jwtUtils) {
        super(Config.class);
        this.routerValidator = routerValidator;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public String name() {
        return "AuthenticationFilter";
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (routerValidator.isSecured(request)) {
                log.debug("Secured route accessed: {}", request.getURI().getPath());

                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    log.warn("Missing Authorization header for secured route: {}", request.getURI().getPath());
                    return onError(exchange, "Missing or invalid authorization token", HttpStatus.UNAUTHORIZED);
                }

                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    log.warn("Authorization header does not start with Bearer: {}", request.getURI().getPath());
                    return onError(exchange, "Missing or invalid authorization token", HttpStatus.UNAUTHORIZED);
                }

                String token = authHeader.substring(7).trim();

                if (!jwtUtils.validateToken(token)) {
                    log.warn("Invalid or expired JWT token for path: {}", request.getURI().getPath());
                    return onError(exchange, "Missing or invalid authorization token", HttpStatus.UNAUTHORIZED);
                }

                try {
                    Claims claims = jwtUtils.getClaims(token);
                    String userId = String.valueOf(claims.get("userId"));
                    String email = claims.getSubject();
                    String role = String.valueOf(claims.get("role"));

                    log.debug("JWT validated successfully: userId={}, email={}, role={}", userId, email, role);

                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Email", email)
                            .header("X-User-Role", role)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                } catch (Exception e) {
                    log.error("Error extracting claims from token: {}", e.getMessage());
                    return onError(exchange, "Missing or invalid authorization token", HttpStatus.UNAUTHORIZED);
                }
            }

            return chain.filter(exchange);
        };
    }

    /**
     * Mutates the response to return a non-blocking 401 Unauthorized payload with a clean JSON body.
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getPath();
        String timestamp = LocalDateTime.now().toString();

        String jsonResponse = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                timestamp, httpStatus.value(), httpStatus.getReasonPhrase(), message, path
        );

        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // Optional configuration parameters for route customization
    }
}

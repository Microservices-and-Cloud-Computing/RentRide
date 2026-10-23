package com.rentride.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Reactive global filter that provides structured request/response logging and
 * distributed-tracing support for every request passing through the API Gateway.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li><strong>Correlation ID</strong> – Reads {@code X-Correlation-ID} from the
 *       incoming request.  If absent, a new UUID is generated and injected into the
 *       mutated request so that downstream services inherit it.</li>
 *   <li><strong>Inbound logging</strong> – Logs the HTTP method, request path, and
 *       client remote address at {@code DEBUG} level before routing.</li>
 *   <li><strong>Outbound logging</strong> – Uses a {@code doOnSuccess} hook on the
 *       downstream Mono to log the HTTP response status code after the response is
 *       committed.  This approach is fully non-blocking and Netty-safe.</li>
 * </ol>
 *
 * <p><strong>Important:</strong> This class uses only reactive Spring WebFlux types
 * ({@link ServerHttpRequest}, {@link ServerHttpResponse}, {@link ServerWebExchange}).
 * Never import {@code javax.servlet.*} or {@code jakarta.servlet.*} here.
 */
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingGlobalFilter.class);

    /** Header name used to propagate a unique correlation ID across services. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    /**
     * Executes the filter logic in a fully reactive, non-blocking manner.
     *
     * @param exchange the current server exchange (request + response context)
     * @param chain    the remaining filter chain to delegate to
     * @return a {@link Mono} that completes when the exchange has been fully handled
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();

        // ── 1. Resolve / generate the Correlation ID ─────────────────────────
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String resolvedCorrelationId = correlationId;

        // ── 2. Mutate the request to forward the Correlation ID downstream ────
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, resolvedCorrelationId)
                .build();

        // ── 3. Log inbound request details ────────────────────────────────────
        String method     = mutatedRequest.getMethod().name();
        String path       = mutatedRequest.getURI().getPath();
        InetSocketAddress remoteAddress = mutatedRequest.getRemoteAddress();
        String clientIp   = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";

        log.debug(
                "[INBOUND]  correlationId={} | method={} | path={} | clientIp={}",
                resolvedCorrelationId, method, path, clientIp
        );

        // ── 4. Continue the chain and log the response on completion ──────────
        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doOnSuccess(aVoid -> {
                    ServerHttpResponse response = exchange.getResponse();
                    int statusCode = response.getStatusCode() != null
                            ? response.getStatusCode().value()
                            : -1;
                    log.debug(
                            "[OUTBOUND] correlationId={} | method={} | path={} | status={}",
                            resolvedCorrelationId, method, path, statusCode
                    );
                });
    }

    /**
     * Determines the filter execution order relative to other {@link GlobalFilter}
     * beans.  A value of {@link Ordered#HIGHEST_PRECEDENCE} ensures this logging
     * filter runs first so that all subsequent filters are covered by the tracing
     * context.
     *
     * @return the filter order value
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

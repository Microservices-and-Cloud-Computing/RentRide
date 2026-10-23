package com.rentride.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Global CORS configuration for the API Gateway (Spring WebFlux / Reactive stack).
 *
 * <p>This {@link CorsWebFilter} bean is the programmatic complement to the declarative
 * {@code spring.cloud.gateway.globalcors} block defined in {@code application.yml}.
 * Both are registered so that CORS headers are applied consistently whether the request
 * is matched to a route or falls through to the default handler.
 *
 * <p><strong>Note:</strong> Never import {@code javax.servlet} or {@code jakarta.servlet}
 * types here – Spring Cloud Gateway runs on Netty via Project Reactor (WebFlux), not a
 * Servlet container.
 */
@Configuration
public class CorsConfig {

    /**
     * Registers a {@link CorsWebFilter} that applies the CORS policy to every path
     * ({@code /**}) processed by the reactive gateway.
     *
     * <p>Policy summary:
     * <ul>
     *   <li>Allowed origins  : React ({@code localhost:3000}) and Angular ({@code localhost:4200}) dev servers</li>
     *   <li>Allowed methods  : GET, POST, PUT, DELETE, PATCH, OPTIONS</li>
     *   <li>Allowed headers  : all ({@code *})</li>
     *   <li>Exposed headers  : Authorization, Link, X-Total-Count, X-Correlation-ID</li>
     *   <li>Allow credentials: {@code true}</li>
     *   <li>Pre-flight cache : 3600 s (1 h)</li>
     * </ul>
     *
     * @return a fully configured {@link CorsWebFilter} Spring bean
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        // ── Allowed Origins ────────────────────────────────────────────────────
        // Use specific origins (not "*") when allow-credentials is true,
        // because browsers reject "credentials + wildcard origin" combinations.
        corsConfiguration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",   // React / Next.js dev server
                "http://localhost:4200"    // Angular dev server
        ));

        // ── Allowed HTTP Methods ───────────────────────────────────────────────
        corsConfiguration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // ── Allowed Request Headers ────────────────────────────────────────────
        corsConfiguration.setAllowedHeaders(List.of("*"));

        // ── Exposed Response Headers ───────────────────────────────────────────
        // Headers the browser JavaScript is allowed to read from the response.
        corsConfiguration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Link",
                "X-Total-Count",
                "X-Correlation-ID"
        ));

        // ── Credentials & Pre-flight Cache ────────────────────────────────────
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setMaxAge(3600L);  // seconds — browsers cache pre-flight for 1 h

        // ── Register the policy for all paths ─────────────────────────────────
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsWebFilter(source);
    }
}

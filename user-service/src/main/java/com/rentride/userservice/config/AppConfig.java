package com.rentride.userservice.config;

import org.springframework.context.annotation.Configuration;

/**
 * Placeholder configuration class for the User Microservice.
 *
 * <p>This class will house application-wide bean definitions as the service grows.
 * Planned additions include:
 * <ul>
 *   <li>Password encoder bean ({@code BCryptPasswordEncoder}) for Commit 05.</li>
 *   <li>Spring Security configuration and JWT filter chain for Commit 05.</li>
 *   <li>Jackson {@code ObjectMapper} customisation (ISO-8601 date serialisation, etc.).</li>
 * </ul>
 *
 * <p>The class intentionally has no bean methods at this scaffolding stage to keep
 * the compile surface minimal while the database connection is verified.
 */
@Configuration
public class AppConfig {
    // Beans will be added incrementally in subsequent commits.
}

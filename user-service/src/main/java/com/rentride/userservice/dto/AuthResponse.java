package com.rentride.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication response payload returning issued JWT tokens and user profile.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Authentication response containing access/refresh tokens and profile")
public class AuthResponse {

    @Schema(description = "JWT Bearer access token string (valid for 24 hours)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "JWT refresh token string (valid for 7 days)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Builder.Default
    @Schema(description = "Token authentication scheme", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Access token lifespan in milliseconds", example = "86400000")
    private Long expiresIn;

    @Schema(description = "Basic authenticated user profile details")
    private UserResponse user;
}

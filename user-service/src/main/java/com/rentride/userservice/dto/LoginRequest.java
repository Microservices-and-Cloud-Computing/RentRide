package com.rentride.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for user login authentication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User credential login payload")
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Schema(description = "Registered user account email", example = "kasun.customer@rentride.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "Account plaintext password for verification", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}

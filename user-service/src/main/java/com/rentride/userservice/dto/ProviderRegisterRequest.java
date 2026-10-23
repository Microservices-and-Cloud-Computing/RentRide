package com.rentride.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for vehicle rental provider registration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Vehicle rental provider account registration payload")
public class ProviderRegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must not exceed 150 characters")
    @Schema(description = "Full legal name of business owner/representative", example = "Nimal Fernando", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "Official business email address used for login", example = "nimal.provider@rentride.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Secure password (minimum 8 characters)", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Phone number format is invalid")
    @Schema(description = "Business contact phone number", example = "+94779876543", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;

    @NotBlank(message = "Business name is required")
    @Size(max = 150, message = "Business name must not exceed 150 characters")
    @Schema(description = "Registered trade or commercial business name", example = "Lanka Auto Rentals", requiredMode = Schema.RequiredMode.REQUIRED)
    private String businessName;

    @NotBlank(message = "Business registration number is required")
    @Size(max = 100, message = "Business registration number must not exceed 100 characters")
    @Schema(description = "Government official business registration certificate number", example = "PV123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String businessRegistrationNumber;

    @Size(max = 100, message = "Tax identification number must not exceed 100 characters")
    @Schema(description = "Official tax identification number (TIN)", example = "TIN987654")
    private String taxIdentificationNumber;
}

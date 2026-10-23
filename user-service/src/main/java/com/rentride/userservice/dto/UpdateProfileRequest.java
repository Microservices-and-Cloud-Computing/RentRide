package com.rentride.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for updating the authenticated user's profile.
 * Only profile fields are editable; credentials, roles, and status are strictly excluded.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must not exceed 150 characters")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Size(max = 30, message = "Phone number must not exceed 30 characters")
    private String phoneNumber;

    /**
     * Optional business name, updatable by providers.
     */
    @Size(max = 150, message = "Business name must not exceed 150 characters")
    private String businessName;

    /**
     * Optional driver license number, updatable by customers.
     */
    @Size(max = 100, message = "Driver license number must not exceed 100 characters")
    private String driverLicenseNumber;
}

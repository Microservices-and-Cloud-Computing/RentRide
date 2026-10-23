package com.rentride.userservice.dto;

import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard data transfer object representing user details.
 * Explicitly excludes sensitive fields such as password hash.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User profile response payload")
public class UserResponse {

    @Schema(description = "Unique platform identifier of user", example = "1")
    private Long id;

    @Schema(description = "Full name of user", example = "Kasun Perera")
    private String fullName;

    @Schema(description = "Registered email address", example = "kasun.customer@rentride.com")
    private String email;

    @Schema(description = "Contact phone number", example = "+94771234567")
    private String phoneNumber;

    @Schema(description = "Assigned platform role", example = "ROLE_CUSTOMER")
    private Role role;

    @Schema(description = "Current account status", example = "ACTIVE")
    private UserStatus status;

    @Schema(description = "Registered business entity name (providers only)", example = "Lanka Auto Rentals")
    private String businessName;

    @Schema(description = "Driver license identifier (customers only)", example = "B1234567")
    private String driverLicenseNumber;

    @Schema(description = "Timestamp when user account was created", example = "2026-10-22T10:15:30")
    private LocalDateTime createdAt;

    /**
     * Converts a {@link User} entity to {@link UserResponse}.
     *
     * @param user the entity to map
     * @return populated {@link UserResponse} DTO
     */
    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .businessName(user.getBusinessName())
                .driverLicenseNumber(user.getDriverLicenseNumber())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

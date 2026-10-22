package com.rentride.userservice.dto;

import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.entity.UserStatus;
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
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Role role;
    private UserStatus status;
    private String businessName;
    private String driverLicenseNumber;
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

package com.rentride.userservice.dto;

import com.rentride.userservice.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for admin-only user account status updates (activate/suspend/verify).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatusUpdateRequest {

    @NotNull(message = "User status is required")
    private UserStatus status;
}

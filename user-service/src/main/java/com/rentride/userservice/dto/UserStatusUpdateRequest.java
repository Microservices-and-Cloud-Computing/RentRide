package com.rentride.userservice.dto;

import com.rentride.userservice.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "User account status modification payload (Admin only)")
public class UserStatusUpdateRequest {

    @NotNull(message = "User status is required")
    @Schema(description = "New account status to apply", example = "SUSPENDED", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserStatus status;
}

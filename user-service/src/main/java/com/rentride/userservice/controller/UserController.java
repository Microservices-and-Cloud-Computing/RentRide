package com.rentride.userservice.controller;

import com.rentride.userservice.dto.ErrorResponse;
import com.rentride.userservice.dto.PaginatedUserResponse;
import com.rentride.userservice.dto.UpdateProfileRequest;
import com.rentride.userservice.dto.UserResponse;
import com.rentride.userservice.dto.UserStatusUpdateRequest;
import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.UserStatus;
import com.rentride.userservice.exception.AuthenticationFailedException;
import com.rentride.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing user profile management, user lookups,
 * and administrative role-based access control endpoints.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "Endpoints for profile operations and administrative user moderation")
public class UserController {

    private final UserService userService;

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @param userDetails authenticated user details from security context
     * @return {@link UserResponse} representing authenticated user
     */
    @GetMapping("/me")
    @Operation(
            summary = "Get current authenticated user profile",
            description = "Returns profile details of the user associated with the provided JWT Bearer token."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated - valid Bearer token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new AuthenticationFailedException("User is not authenticated");
        }

        String email = userDetails.getUsername();
        log.info("Fetching profile for authenticated user: {}", email);
        UserResponse response = userService.getCurrentUserProfile(email);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the editable profile fields of the currently authenticated user.
     *
     * @param userDetails authenticated user details from security context
     * @param request     validated profile update request
     * @return updated {@link UserResponse}
     */
    @PutMapping("/me")
    @Operation(
            summary = "Update current user profile",
            description = "Updates editable profile fields (name, phone, driver license, or business name) according to role constraints."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or malformed payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated - valid Bearer token required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {

        if (userDetails == null) {
            throw new AuthenticationFailedException("User is not authenticated");
        }

        String email = userDetails.getUsername();
        log.info("Updating profile for authenticated user: {}", email);
        UserResponse response = userService.updateCurrentUserProfile(email, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a user by their database ID.
     * Allowed only if caller has ROLE_ADMIN or is the user themselves.
     *
     * @param id target user ID
     * @return {@link UserResponse}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    @Operation(
            summary = "Get user by ID",
            description = "Retrieves user profile details by ID. Restricted to the user themselves or users with ROLE_ADMIN."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller does not have permission to view this profile",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found with specified ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "Database ID of user to retrieve", example = "1")
            @PathVariable Long id) {
        log.info("Fetching user by id: {}", id);
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated list of all users on the platform with optional filters.
     * Restricted to administrators only.
     *
     * @param page   zero-based page number (default: 0)
     * @param size   page size (default: 10)
     * @param role   optional role filter
     * @param status optional status filter
     * @return {@link PaginatedUserResponse}
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all users (Admin only)",
            description = "Returns a paginated list of all platform users with optional role and status filters. Restricted to ROLE_ADMIN."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of users retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PaginatedUserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin privilege required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaginatedUserResponse> getAllUsers(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size limit", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by platform role", example = "ROLE_CUSTOMER")
            @RequestParam(required = false) Role role,
            @Parameter(description = "Filter by account status", example = "ACTIVE")
            @RequestParam(required = false) UserStatus status) {

        log.info("Admin query for users: page={}, size={}, role={}, status={}", page, size, role, status);
        Page<UserResponse> userPage = userService.getAllUsers(page, size, role, status);
        return ResponseEntity.ok(PaginatedUserResponse.fromPage(userPage));
    }

    /**
     * Updates the account status of a user (e.g., ACTIVE, SUSPENDED).
     * Restricted to administrators only.
     *
     * @param id      target user ID
     * @param request validated status update request
     * @return updated {@link UserResponse}
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update user account status (Admin only)",
            description = "Applies a status update (ACTIVE, SUSPENDED, PENDING_VERIFICATION) to the specified user. Restricted to ROLE_ADMIN."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User status updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status value provided",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin privilege required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found with specified ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> updateUserStatus(
            @Parameter(description = "Database ID of user whose status is being modified", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request) {

        log.info("Admin updating status for user id: {} to {}", id, request.getStatus());
        UserResponse response = userService.updateUserStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }
}

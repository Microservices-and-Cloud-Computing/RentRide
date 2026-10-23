package com.rentride.userservice.controller;

import com.rentride.userservice.dto.PaginatedUserResponse;
import com.rentride.userservice.dto.UpdateProfileRequest;
import com.rentride.userservice.dto.UserResponse;
import com.rentride.userservice.dto.UserStatusUpdateRequest;
import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.UserStatus;
import com.rentride.userservice.exception.AuthenticationFailedException;
import com.rentride.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
public class UserController {

    private final UserService userService;

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @param userDetails authenticated user details from security context
     * @return {@link UserResponse} representing authenticated user
     */
    @GetMapping("/me")
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
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
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
    public ResponseEntity<PaginatedUserResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Role role,
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
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request) {

        log.info("Admin updating status for user id: {} to {}", id, request.getStatus());
        UserResponse response = userService.updateUserStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }
}

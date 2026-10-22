package com.rentride.userservice.controller;

import com.rentride.userservice.dto.UserResponse;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.exception.AuthenticationFailedException;
import com.rentride.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authenticated user profile operations.
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

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("User not found for email: " + email));

        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
}

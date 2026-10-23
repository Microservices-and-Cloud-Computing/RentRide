package com.rentride.userservice.controller;

import com.rentride.userservice.dto.*;
import com.rentride.userservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing user registration, credential validation, login,
 * and JWT refresh endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for user registration, token generation, and credential authentication")
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new customer account on the platform.
     * Supports both /api/v1/auth/register and /api/v1/auth/register/customer.
     *
     * @param request validated customer registration payload
     * @return {@code 201 Created} with registered customer details
     */
    @PostMapping({"/register", "/register/customer"})
    @Operation(
            summary = "Register customer account",
            description = "Creates a new customer profile with ROLE_CUSTOMER and ACTIVE status. Excludes password from response."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer registered successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Input validation failed or malformed body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email address already registered on the platform",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> registerCustomer(
            @Valid @RequestBody CustomerRegisterRequest request) {
        log.info("Received customer registration request for email: {}", request.getEmail());
        UserResponse response = authService.registerCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Registers a new vehicle rental provider account on the platform.
     *
     * @param request validated provider registration payload
     * @return {@code 201 Created} with registered provider details
     */
    @PostMapping("/register/provider")
    @Operation(
            summary = "Register vehicle provider account",
            description = "Creates a new provider account with commercial registration details, ROLE_PROVIDER, and ACTIVE status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Provider registered successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Input validation failed or malformed body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email address already registered on the platform",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> registerProvider(
            @Valid @RequestBody ProviderRegisterRequest request) {
        log.info("Received provider registration request for email: {}", request.getEmail());
        UserResponse response = authService.registerProvider(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates user credentials and issues JWT access and refresh tokens.
     *
     * @param request validated login credentials payload
     * @return {@code 200 OK} with tokens and user profile
     */
    @PostMapping("/login")
    @Operation(
            summary = "User login and token generation",
            description = "Validates user email and password credentials, returning a 24-hour JWT access token and 7-day refresh token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Missing or malformed login credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid email or password / Account suspended",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("Received login request for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     *
     * @param request validated refresh token payload
     * @return {@code 200 OK} with newly issued access token
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh expired access token",
            description = "Validates the provided 7-day refresh token signature and issues a fresh 24-hour access token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Missing or malformed refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or tampered refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Received token refresh request");
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
}

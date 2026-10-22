package com.rentride.userservice.controller;

import com.rentride.userservice.dto.*;
import com.rentride.userservice.service.AuthService;
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
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new customer account on the platform.
     *
     * @param request validated customer registration payload
     * @return {@code 201 Created} with registered customer details
     */
    @PostMapping("/register/customer")
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
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Received token refresh request");
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
}

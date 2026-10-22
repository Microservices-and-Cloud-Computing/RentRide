package com.rentride.userservice.service;

import com.rentride.userservice.dto.*;

/**
 * Service contract for user registration, credential validation, login, and token operations.
 */
public interface AuthService {

    /**
     * Registers a new customer with the platform.
     *
     * @param request the customer registration payload
     * @return {@link UserResponse} containing persisted customer details
     */
    UserResponse registerCustomer(CustomerRegisterRequest request);

    /**
     * Registers a new vehicle rental provider with business details.
     *
     * @param request the provider registration payload
     * @return {@link UserResponse} containing persisted provider details
     */
    UserResponse registerProvider(ProviderRegisterRequest request);

    /**
     * Authenticates user credentials and generates JWT access and refresh tokens.
     *
     * @param request login credentials payload
     * @return {@link AuthResponse} containing issued tokens and user profile
     */
    AuthResponse login(LoginRequest request);

    /**
     * Validates a refresh token and generates a new access token.
     *
     * @param request refresh token payload
     * @return {@link AuthResponse} containing refreshed access token
     */
    AuthResponse refreshToken(RefreshTokenRequest request);
}

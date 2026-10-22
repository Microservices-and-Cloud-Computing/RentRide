package com.rentride.userservice.service;

import com.rentride.userservice.dto.CustomerRegisterRequest;
import com.rentride.userservice.dto.ProviderRegisterRequest;
import com.rentride.userservice.dto.UserResponse;

/**
 * Service contract for user registration and authentication operations.
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
}

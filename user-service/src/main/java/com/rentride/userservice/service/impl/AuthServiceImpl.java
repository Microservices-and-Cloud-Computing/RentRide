package com.rentride.userservice.service.impl;

import com.rentride.userservice.dto.CustomerRegisterRequest;
import com.rentride.userservice.dto.ProviderRegisterRequest;
import com.rentride.userservice.dto.UserResponse;
import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.entity.UserStatus;
import com.rentride.userservice.exception.EmailAlreadyExistsException;
import com.rentride.userservice.repository.UserRepository;
import com.rentride.userservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AuthService} managing user registration workflows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse registerCustomer(CustomerRegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        log.info("Processing customer registration request for email: {}", normalizedEmail);

        if (Boolean.TRUE.equals(userRepository.existsByEmail(normalizedEmail))) {
            log.warn("Registration rejected: Email '{}' already exists", normalizedEmail);
            throw new EmailAlreadyExistsException("Email '" + normalizedEmail + "' is already registered");
        }

        User customer = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber().trim())
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .driverLicenseNumber(request.getDriverLicenseNumber().trim())
                .build();

        User savedUser = userRepository.save(customer);
        log.info("Customer registered successfully with id: {}, email: {}", savedUser.getId(), savedUser.getEmail());

        return UserResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional
    public UserResponse registerProvider(ProviderRegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        log.info("Processing provider registration request for email: {}", normalizedEmail);

        if (Boolean.TRUE.equals(userRepository.existsByEmail(normalizedEmail))) {
            log.warn("Registration rejected: Email '{}' already exists", normalizedEmail);
            throw new EmailAlreadyExistsException("Email '" + normalizedEmail + "' is already registered");
        }

        User provider = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber().trim())
                .role(Role.ROLE_PROVIDER)
                .status(UserStatus.ACTIVE)
                .businessName(request.getBusinessName().trim())
                .businessRegistrationNumber(request.getBusinessRegistrationNumber().trim())
                .taxIdentificationNumber(request.getTaxIdentificationNumber() != null ? request.getTaxIdentificationNumber().trim() : null)
                .build();

        User savedUser = userRepository.save(provider);
        log.info("Provider registered successfully with id: {}, email: {}", savedUser.getId(), savedUser.getEmail());

        return UserResponse.fromEntity(savedUser);
    }
}

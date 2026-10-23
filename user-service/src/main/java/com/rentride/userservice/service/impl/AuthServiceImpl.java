package com.rentride.userservice.service.impl;

import com.rentride.userservice.dto.*;
import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.entity.UserStatus;
import com.rentride.userservice.exception.AuthenticationFailedException;
import com.rentride.userservice.exception.EmailAlreadyExistsException;
import com.rentride.userservice.repository.UserRepository;
import com.rentride.userservice.security.JwtService;
import com.rentride.userservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AuthService} managing registration, authentication,
 * and JWT token lifecycle operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

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

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        log.info("Authenticating user credentials for email: {}", normalizedEmail);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthenticationFailedException("User not found with email: " + normalizedEmail));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            log.warn("Authentication blocked: user account is suspended: {}", normalizedEmail);
            throw new AuthenticationFailedException("Your account has been suspended. Please contact support.");
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("Authentication successful: id={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .user(UserResponse.fromEntity(user))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtService.isTokenValid(refreshToken)) {
            log.warn("Refresh token rejected: invalid or expired signature");
            throw new AuthenticationFailedException("Invalid or expired refresh token");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("User not found for provided refresh token"));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthenticationFailedException("Your account has been suspended");
        }

        String newAccessToken = jwtService.generateToken(user);
        log.info("Issued refreshed access token for user: {}", email);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .user(UserResponse.fromEntity(user))
                .build();
    }
}

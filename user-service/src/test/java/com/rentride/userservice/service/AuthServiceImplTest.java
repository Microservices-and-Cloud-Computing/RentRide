package com.rentride.userservice.service;

import com.rentride.userservice.dto.*;
import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.entity.UserStatus;
import com.rentride.userservice.exception.AuthenticationFailedException;
import com.rentride.userservice.exception.EmailAlreadyExistsException;
import com.rentride.userservice.repository.UserRepository;
import com.rentride.userservice.security.JwtService;
import com.rentride.userservice.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("registerCustomer: Success should hash password, assign ROLE_CUSTOMER and ACTIVE status")
    void testRegisterCustomer_Success() {
        CustomerRegisterRequest request = CustomerRegisterRequest.builder()
                .fullName("Alice Customer")
                .email("alice@rentride.com")
                .password("Password123!")
                .phoneNumber("+1234567890")
                .driverLicenseNumber("DL-ALICE-123")
                .build();

        when(userRepository.existsByEmail("alice@rentride.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$10$hashedPassword");

        User savedUser = User.builder()
                .id(1L)
                .fullName("Alice Customer")
                .email("alice@rentride.com")
                .password("$2a$10$hashedPassword")
                .phoneNumber("+1234567890")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .driverLicenseNumber("DL-ALICE-123")
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = authService.registerCustomer(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("alice@rentride.com");
        assertThat(response.getRole()).isEqualTo(Role.ROLE_CUSTOMER);
        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.getDriverLicenseNumber()).isEqualTo("DL-ALICE-123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User capturedUser = captor.getValue();
        assertThat(capturedUser.getPassword()).isEqualTo("$2a$10$hashedPassword");
    }

    @Test
    @DisplayName("registerCustomer: Duplicate email should throw EmailAlreadyExistsException")
    void testRegisterCustomer_DuplicateEmail_ThrowsException() {
        CustomerRegisterRequest request = CustomerRegisterRequest.builder()
                .fullName("Alice Customer")
                .email("duplicate@rentride.com")
                .password("Password123!")
                .phoneNumber("+1234567890")
                .driverLicenseNumber("DL-ALICE-123")
                .build();

        when(userRepository.existsByEmail("duplicate@rentride.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.registerCustomer(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("registerProvider: Success should hash password, assign ROLE_PROVIDER and store business details")
    void testRegisterProvider_Success() {
        ProviderRegisterRequest request = ProviderRegisterRequest.builder()
                .fullName("Bob Provider")
                .email("bob@rentride.com")
                .password("SecurePass456!")
                .phoneNumber("+1987654321")
                .businessName("Apex Rental Fleet")
                .businessRegistrationNumber("BRN-998877")
                .taxIdentificationNumber("TIN-112233")
                .build();

        when(userRepository.existsByEmail("bob@rentride.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass456!")).thenReturn("$2a$10$hashedProviderPassword");

        User savedUser = User.builder()
                .id(2L)
                .fullName("Bob Provider")
                .email("bob@rentride.com")
                .password("$2a$10$hashedProviderPassword")
                .phoneNumber("+1987654321")
                .role(Role.ROLE_PROVIDER)
                .status(UserStatus.ACTIVE)
                .businessName("Apex Rental Fleet")
                .businessRegistrationNumber("BRN-998877")
                .taxIdentificationNumber("TIN-112233")
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = authService.registerProvider(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getEmail()).isEqualTo("bob@rentride.com");
        assertThat(response.getRole()).isEqualTo(Role.ROLE_PROVIDER);
        assertThat(response.getBusinessName()).isEqualTo("Apex Rental Fleet");
        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("login: Valid credentials should authenticate and issue access and refresh tokens")
    void testLogin_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("login@rentride.com")
                .password("Password123!")
                .build();

        User user = User.builder()
                .id(10L)
                .email("login@rentride.com")
                .fullName("Login User")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("login@rentride.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("mock.access.token");
        when(jwtService.generateRefreshToken(user)).thenReturn("mock.refresh.token");
        when(jwtService.getExpirationTime()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("mock.refresh.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(86400000L);
        assertThat(response.getUser().getEmail()).isEqualTo("login@rentride.com");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login: Invalid credentials should throw BadCredentialsException")
    void testLogin_BadCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email("login@rentride.com")
                .password("WrongPassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("login: Suspended account should throw AuthenticationFailedException")
    void testLogin_SuspendedAccount() {
        LoginRequest request = LoginRequest.builder()
                .email("suspended@rentride.com")
                .password("Password123!")
                .build();

        User suspendedUser = User.builder()
                .id(11L)
                .email("suspended@rentride.com")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.SUSPENDED)
                .build();

        when(userRepository.findByEmail("suspended@rentride.com")).thenReturn(Optional.of(suspendedUser));

        assertThrows(AuthenticationFailedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("refreshToken: Valid token should generate new access token")
    void testRefreshToken_Success() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid.refresh.token")
                .build();

        User user = User.builder()
                .id(12L)
                .email("token.user@rentride.com")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        when(jwtService.isTokenValid("valid.refresh.token")).thenReturn(true);
        when(jwtService.extractEmail("valid.refresh.token")).thenReturn("token.user@rentride.com");
        when(userRepository.findByEmail("token.user@rentride.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("new.access.token");
        when(jwtService.getExpirationTime()).thenReturn(86400000L);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("valid.refresh.token");
    }

    @Test
    @DisplayName("refreshToken: Invalid token should throw AuthenticationFailedException")
    void testRefreshToken_Invalid() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid.refresh.token")
                .build();

        when(jwtService.isTokenValid("invalid.refresh.token")).thenReturn(false);

        assertThrows(AuthenticationFailedException.class, () -> authService.refreshToken(request));
    }
}

package com.rentride.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentride.userservice.dto.AuthResponse;
import com.rentride.userservice.dto.CustomerRegisterRequest;
import com.rentride.userservice.dto.LoginRequest;
import com.rentride.userservice.dto.UserResponse;
import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.UserStatus;
import com.rentride.userservice.security.CustomUserDetailsService;
import com.rentride.userservice.security.JwtAuthenticationFilter;
import com.rentride.userservice.security.JwtService;
import com.rentride.userservice.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-slice unit tests for {@link AuthController} validating WebMvc request mapping,
 * JSON serialization, and bean validation in isolation from persistence or security services.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("registerCustomer_MissingFields_Returns400: Missing fields return HTTP 400 Bad Request with validation errors")
    void registerCustomer_MissingFields_Returns400() throws Exception {
        CustomerRegisterRequest invalidRequest = CustomerRegisterRequest.builder()
                .fullName("Kasun Perera")
                .email("")
                .password("short")
                .phoneNumber("+94771234567")
                .driverLicenseNumber("B1234567")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.validationErrors.email", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.password", notNullValue()));
    }

    @Test
    @DisplayName("registerCustomer_ValidPayload_Returns201: Valid request returns HTTP 201 Created and matching UserResponse structure")
    void registerCustomer_ValidPayload_Returns201() throws Exception {
        CustomerRegisterRequest validRequest = CustomerRegisterRequest.builder()
                .fullName("Kasun Perera")
                .email("kasun.customer@rentride.com")
                .password("Password123!")
                .phoneNumber("+94771234567")
                .driverLicenseNumber("B1234567")
                .build();

        UserResponse mockResponse = UserResponse.builder()
                .id(1L)
                .fullName("Kasun Perera")
                .email("kasun.customer@rentride.com")
                .phoneNumber("+94771234567")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .driverLicenseNumber("B1234567")
                .createdAt(LocalDateTime.now())
                .build();

        when(authService.registerCustomer(any(CustomerRegisterRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.fullName", is("Kasun Perera")))
                .andExpect(jsonPath("$.email", is("kasun.customer@rentride.com")))
                .andExpect(jsonPath("$.role", is("ROLE_CUSTOMER")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.driverLicenseNumber", is("B1234567")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("login_ValidPayload_Returns200: Valid credentials return HTTP 200 OK and presence of accessToken")
    void login_ValidPayload_Returns200() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("kasun.customer@rentride.com")
                .password("Password123!")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .fullName("Kasun Perera")
                .email("kasun.customer@rentride.com")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        AuthResponse mockAuthResponse = AuthResponse.builder()
                .accessToken("mocked.access.jwt.token")
                .refreshToken("mocked.refresh.jwt.token")
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .user(userResponse)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("mocked.access.jwt.token")))
                .andExpect(jsonPath("$.refreshToken", is("mocked.refresh.jwt.token")))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(86400000)))
                .andExpect(jsonPath("$.user.email", is("kasun.customer@rentride.com")));
    }
}

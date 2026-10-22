package com.rentride.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentride.userservice.dto.AuthResponse;
import com.rentride.userservice.dto.CustomerRegisterRequest;
import com.rentride.userservice.dto.LoginRequest;
import com.rentride.userservice.dto.RefreshTokenRequest;
import com.rentride.userservice.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration and security testing for JWT authentication and protected endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    private static final String EMAIL = "kasun.customer@rentride.com";
    private static final String PASSWORD = "Password123!";

    @BeforeEach
    void setUp() throws Exception {
        CustomerRegisterRequest regRequest = CustomerRegisterRequest.builder()
                .fullName("Kasun Perera")
                .email(EMAIL)
                .password(PASSWORD)
                .phoneNumber("+94771234567")
                .driverLicenseNumber("B1234567")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("testLogin_Success: Returns 200 OK, valid JWT access/refresh tokens and tokenType Bearer")
    void testLogin_Success() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(86400000)))
                .andExpect(jsonPath("$.user.email", is(EMAIL)))
                .andExpect(jsonPath("$.user.role", is("ROLE_CUSTOMER")))
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);

        // Verify well-formed JWT strings (three base64 segments separated by dots)
        assertThat(authResponse.getAccessToken().split("\\.")).hasSize(3);
        assertThat(authResponse.getRefreshToken().split("\\.")).hasSize(3);

        // JWT Token Payload Inspection
        Claims claims = jwtService.extractAllClaims(authResponse.getAccessToken());
        assertThat(claims.getSubject()).isEqualTo(EMAIL);
        assertThat(claims.get("role")).isEqualTo("ROLE_CUSTOMER");
        assertThat(claims.get("userId")).isNotNull();
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    @DisplayName("testLogin_InvalidPassword_Returns401: Wrong password returns 401 with 'Invalid email or password'")
    void testLogin_InvalidPassword_Returns401() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(EMAIL)
                .password("WrongPassword!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("testLogin_NonExistentUser_Returns401: Unregistered email returns 401 Unauthorized")
    void testLogin_NonExistentUser_Returns401() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("unregistered.user@rentride.com")
                .password(PASSWORD)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("testRefreshToken_Success: Valid refreshToken returns HTTP 200 and fresh access token")
    void testRefreshToken_Success() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);

        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken(loginResponse.getRefreshToken())
                .build();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", is(loginResponse.getRefreshToken())))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.email", is(EMAIL)));
    }

    @Test
    @DisplayName("testProtectedMeEndpoint_WithoutToken_Returns403Or401")
    void testProtectedMeEndpoint_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("testProtectedMeEndpoint_WithValidToken_Returns200OK")
    void testProtectedMeEndpoint_WithValidToken_Returns200OK() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + authResponse.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(EMAIL)))
                .andExpect(jsonPath("$.fullName", is("Kasun Perera")))
                .andExpect(jsonPath("$.role", is("ROLE_CUSTOMER")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}

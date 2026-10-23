package com.rentride.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentride.userservice.dto.AuthResponse;
import com.rentride.userservice.dto.CustomerRegisterRequest;
import com.rentride.userservice.dto.LoginRequest;
import com.rentride.userservice.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test suite validating complete authentication flow,
 * registration, login, token extraction, profile retrieval, and security context isolation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete Authentication Flow: Register customer, login, extract accessToken, and query /me profile")
    void testCompleteAuthenticationFlow_Success() throws Exception {
        // Step 1: Execute POST /api/v1/auth/register/customer. Assert HTTP 201.
        CustomerRegisterRequest registerRequest = CustomerRegisterRequest.builder()
                .fullName("Kasun Perera")
                .email("kasun.perera@rentride.com")
                .password("Password123!")
                .phoneNumber("+94771234567")
                .driverLicenseNumber("B1234567")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", is("kasun.perera@rentride.com")))
                .andExpect(jsonPath("$.role", is("ROLE_CUSTOMER")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.password").doesNotExist());

        // Step 2: Execute POST /api/v1/auth/login using newly created credentials. Assert HTTP 200, extract accessToken.
        LoginRequest loginRequest = LoginRequest.builder()
                .email("kasun.perera@rentride.com")
                .password("Password123!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.email", is("kasun.perera@rentride.com")))
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), AuthResponse.class);

        String accessToken = authResponse.getAccessToken();
        assertThat(accessToken).isNotBlank();

        // Step 3: Query GET /api/v1/users/me with header Authorization: Bearer <accessToken>. Assert HTTP 200 and profile identity match.
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("kasun.perera@rentride.com")))
                .andExpect(jsonPath("$.fullName", is("Kasun Perera")))
                .andExpect(jsonPath("$.role", is("ROLE_CUSTOMER")))
                .andExpect(jsonPath("$.driverLicenseNumber", is("B1234567")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Security Context Isolation: Unauthenticated call to /api/v1/users/me returns 401 Unauthorized or 403 Forbidden")
    void testGetMyProfile_Unauthenticated_ReturnsUnauthorizedOrForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Negative Edge Case: Login with invalid password returns HTTP 401 Unauthorized")
    void testLogin_BadCredentials_Returns401() throws Exception {
        CustomerRegisterRequest registerRequest = CustomerRegisterRequest.builder()
                .fullName("Chaminda Silva")
                .email("chaminda@rentride.com")
                .password("Password123!")
                .phoneNumber("+94779876543")
                .driverLicenseNumber("B9876543")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest badLoginRequest = LoginRequest.builder()
                .email("chaminda@rentride.com")
                .password("WrongPassword999!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("Negative Edge Case: Duplicate registration returns HTTP 409 Conflict")
    void testRegisterCustomer_DuplicateEmail_Returns409() throws Exception {
        CustomerRegisterRequest registerRequest = CustomerRegisterRequest.builder()
                .fullName("Nuwan Bandara")
                .email("nuwan@rentride.com")
                .password("Password123!")
                .phoneNumber("+94775554433")
                .driverLicenseNumber("B5554433")
                .build();

        // First registration succeeds
        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Duplicate registration rejected
        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    @DisplayName("Negative Edge Case: Validation rejection on empty and short fields returns HTTP 400 Bad Request")
    void testRegisterCustomer_InvalidPayload_Returns400() throws Exception {
        CustomerRegisterRequest invalidRequest = CustomerRegisterRequest.builder()
                .fullName(" ")
                .email("invalid-email-address")
                .password("short")
                .phoneNumber("")
                .driverLicenseNumber("")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.validationErrors.email", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.password", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.fullName", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.phoneNumber", notNullValue()));
    }
}

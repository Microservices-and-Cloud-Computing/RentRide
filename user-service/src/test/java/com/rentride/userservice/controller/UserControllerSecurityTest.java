package com.rentride.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentride.userservice.dto.LoginRequest;
import com.rentride.userservice.dto.UpdateProfileRequest;
import com.rentride.userservice.dto.UserStatusUpdateRequest;
import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.entity.UserStatus;
import com.rentride.userservice.repository.UserRepository;
import com.rentride.userservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security and RBAC verification integration tests for {@link UserController}.
 * Verifies profile retrieval, profile updates, and admin-only management endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String RAW_PASSWORD = "Password123!";

    private User adminUser;
    private User customerUser;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        adminUser = userRepository.save(User.builder()
                .fullName("System Administrator")
                .email("admin.security@rentride.com")
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .phoneNumber("+94770000001")
                .role(Role.ROLE_ADMIN)
                .status(UserStatus.ACTIVE)
                .build());

        customerUser = userRepository.save(User.builder()
                .fullName("Kasun Customer")
                .email("kasun.customer@rentride.com")
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .phoneNumber("+94771234567")
                .driverLicenseNumber("B1234567")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        adminToken = jwtService.generateToken(adminUser);
        customerToken = jwtService.generateToken(customerUser);
    }

    @Test
    @DisplayName("testGetMyProfile_Authenticated_Returns200: Returns 200 OK with correct profile matching token")
    void testGetMyProfile_Authenticated_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(customerUser.getId().intValue())))
                .andExpect(jsonPath("$.email", is("kasun.customer@rentride.com")))
                .andExpect(jsonPath("$.fullName", is("Kasun Customer")))
                .andExpect(jsonPath("$.phoneNumber", is("+94771234567")))
                .andExpect(jsonPath("$.role", is("ROLE_CUSTOMER")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.driverLicenseNumber", is("B1234567")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("testUpdateMyProfile_Success: PUT updates phone and name, changes reflected in DB")
    void testUpdateMyProfile_Success() throws Exception {
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .fullName("Kasun Perera Updated")
                .phoneNumber("+94779876543")
                .driverLicenseNumber("B7654321")
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Kasun Perera Updated")))
                .andExpect(jsonPath("$.phoneNumber", is("+94779876543")))
                .andExpect(jsonPath("$.driverLicenseNumber", is("B7654321")));

        // Verify changes in database
        User updated = userRepository.findById(customerUser.getId()).orElseThrow();
        assertThat(updated.getFullName()).isEqualTo("Kasun Perera Updated");
        assertThat(updated.getPhoneNumber()).isEqualTo("+94779876543");
        assertThat(updated.getDriverLicenseNumber()).isEqualTo("B7654321");
    }

    @Test
    @DisplayName("testGetAllUsers_CustomerRole_Returns403Forbidden: Non-admin caller receives 403 Forbidden")
    void testGetAllUsers_CustomerRole_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("testGetAllUsers_AdminRole_Returns200: Admin receives 200 OK and paginated user list")
    void testGetAllUsers_AdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.pageNo", is(0)))
                .andExpect(jsonPath("$.pageSize", is(10)))
                .andExpect(jsonPath("$.last", is(true)));
    }

    @Test
    @DisplayName("testUpdateUserStatus_AdminRole_Success: Admin suspends user and subsequent login fails")
    void testUpdateUserStatus_AdminRole_Success() throws Exception {
        UserStatusUpdateRequest statusRequest = UserStatusUpdateRequest.builder()
                .status(UserStatus.SUSPENDED)
                .build();

        // 1. Issue PATCH as Admin to suspend account
        mockMvc.perform(patch("/api/v1/users/" + customerUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(customerUser.getId().intValue())))
                .andExpect(jsonPath("$.status", is("SUSPENDED")));

        // 2. Verify subsequent login attempt by suspended user fails with 401 Unauthorized
        LoginRequest loginRequest = LoginRequest.builder()
                .email("kasun.customer@rentride.com")
                .password(RAW_PASSWORD)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }
}

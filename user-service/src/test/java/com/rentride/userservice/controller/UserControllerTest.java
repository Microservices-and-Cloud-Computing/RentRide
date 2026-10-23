package com.rentride.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration and RBAC authorization verification tests for {@link UserController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

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

    private User adminUser;
    private User customer1;
    private User customer2;

    private String adminToken;
    private String customer1Token;
    private String customer2Token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        adminUser = userRepository.save(User.builder()
                .fullName("System Admin")
                .email("admin@rentride.com")
                .password(passwordEncoder.encode("AdminPass123!"))
                .phoneNumber("+94770000001")
                .role(Role.ROLE_ADMIN)
                .status(UserStatus.ACTIVE)
                .build());

        customer1 = userRepository.save(User.builder()
                .fullName("Kasun Customer")
                .email("kasun.c1@rentride.com")
                .password(passwordEncoder.encode("Password123!"))
                .phoneNumber("+94771111111")
                .driverLicenseNumber("B1111111")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        customer2 = userRepository.save(User.builder()
                .fullName("Amal Customer")
                .email("amal.c2@rentride.com")
                .password(passwordEncoder.encode("Password123!"))
                .phoneNumber("+94772222222")
                .driverLicenseNumber("B2222222")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        adminToken = jwtService.generateToken(adminUser);
        customer1Token = jwtService.generateToken(customer1);
        customer2Token = jwtService.generateToken(customer2);
    }

    @Test
    @DisplayName("GET /api/v1/users/me: Returns current user profile with valid JWT")
    void testGetCurrentUser_Success() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("kasun.c1@rentride.com")))
                .andExpect(jsonPath("$.fullName", is("Kasun Customer")))
                .andExpect(jsonPath("$.role", is("ROLE_CUSTOMER")))
                .andExpect(jsonPath("$.driverLicenseNumber", is("B1111111")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/users/me: Unauthenticated request returns 403 Forbidden")
    void testGetCurrentUser_Unauthenticated_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/users/me: Successfully updates authenticated customer profile")
    void testUpdateCurrentUser_Success() throws Exception {
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .fullName("Kasun Perera Updated")
                .phoneNumber("+94779998888")
                .driverLicenseNumber("B9998888")
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Kasun Perera Updated")))
                .andExpect(jsonPath("$.phoneNumber", is("+94779998888")))
                .andExpect(jsonPath("$.driverLicenseNumber", is("B9998888")));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id}: Allowed for user themselves")
    void testGetUserById_SelfAccess_Success() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + customer1.getId())
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(customer1.getId().intValue())))
                .andExpect(jsonPath("$.email", is("kasun.c1@rentride.com")));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id}: Allowed for administrator")
    void testGetUserById_AdminAccess_Success() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + customer1.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(customer1.getId().intValue())))
                .andExpect(jsonPath("$.email", is("kasun.c1@rentride.com")));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id}: Denied (403) for other non-admin users")
    void testGetUserById_OtherUser_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + customer1.getId())
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id}: Admin accessing non-existent user returns 404")
    void testGetUserById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/users/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    @DisplayName("GET /api/v1/users: Admin gets paginated list of users")
    void testGetAllUsers_AdminAccess_Success() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.pageNo", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/users: Filtered by role and status")
    void testGetAllUsers_WithFilters_Success() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("role", "ROLE_CUSTOMER")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].role", is("ROLE_CUSTOMER")));
    }

    @Test
    @DisplayName("GET /api/v1/users: Non-admin caller receives 403 Forbidden")
    void testGetAllUsers_CustomerAccess_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/status: Admin updates user status to SUSPENDED")
    void testUpdateUserStatus_Admin_Success() throws Exception {
        UserStatusUpdateRequest statusRequest = UserStatusUpdateRequest.builder()
                .status(UserStatus.SUSPENDED)
                .build();

        mockMvc.perform(patch("/api/v1/users/" + customer1.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(customer1.getId().intValue())))
                .andExpect(jsonPath("$.status", is("SUSPENDED")));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/status: Non-admin caller receives 403 Forbidden")
    void testUpdateUserStatus_Customer_Returns403() throws Exception {
        UserStatusUpdateRequest statusRequest = UserStatusUpdateRequest.builder()
                .status(UserStatus.SUSPENDED)
                .build();

        mockMvc.perform(patch("/api/v1/users/" + customer1.getId() + "/status")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }
}

package com.rentride.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentride.userservice.dto.CustomerRegisterRequest;
import com.rentride.userservice.dto.ProviderRegisterRequest;
import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.entity.UserStatus;
import com.rentride.userservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration and API verification tests for {@link AuthController}.
 *
 * <p>Verifies customer and provider registration, input validation, duplicate email conflicts,
 * and database BCrypt password encryption against the test profile.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("testRegisterCustomer_Success: HTTP 201, ROLE_CUSTOMER, ACTIVE, non-null ID, password absent")
    void testRegisterCustomer_Success() throws Exception {
        CustomerRegisterRequest request = CustomerRegisterRequest.builder()
                .fullName("Kasun Perera")
                .email("kasun.customer@rentride.com")
                .password("Password123!")
                .phoneNumber("+94771234567")
                .driverLicenseNumber("B1234567")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.fullName", is("Kasun Perera")))
                .andExpect(jsonPath("$.email", is("kasun.customer@rentride.com")))
                .andExpect(jsonPath("$.role", is("ROLE_CUSTOMER")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.driverLicenseNumber", is("B1234567")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("testRegisterProvider_Success: HTTP 201, ROLE_PROVIDER, business info populated")
    void testRegisterProvider_Success() throws Exception {
        ProviderRegisterRequest request = ProviderRegisterRequest.builder()
                .fullName("Saman Silva")
                .email("saman.provider@rentride.com")
                .password("Password123!")
                .phoneNumber("+94719876543")
                .businessName("Lanka Auto Rentals")
                .businessRegistrationNumber("PV-98765")
                .taxIdentificationNumber("TIN-112233")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/provider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.fullName", is("Saman Silva")))
                .andExpect(jsonPath("$.email", is("saman.provider@rentride.com")))
                .andExpect(jsonPath("$.role", is("ROLE_PROVIDER")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.businessName", is("Lanka Auto Rentals")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("testRegisterCustomer_DuplicateEmail_ThrowsConflict: HTTP 409 Conflict with descriptive message")
    void testRegisterCustomer_DuplicateEmail_ThrowsConflict() throws Exception {
        CustomerRegisterRequest request = CustomerRegisterRequest.builder()
                .fullName("Kasun Perera")
                .email("duplicate.customer@rentride.com")
                .password("Password123!")
                .phoneNumber("+94771234567")
                .driverLicenseNumber("B1234567")
                .build();

        // First registration succeeds
        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second registration with same email must fail with 409 Conflict
        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    @DisplayName("testValidationFailures: Missing fields, blank email, short password return HTTP 400 Bad Request")
    void testValidationFailures() throws Exception {
        // Test 1: Blank email and password under 8 characters
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
                .andExpect(jsonPath("$.validationErrors.password", containsString("at least 8 characters")));

        // Test 2: Invalid email format
        CustomerRegisterRequest invalidEmailRequest = CustomerRegisterRequest.builder()
                .fullName("Kasun Perera")
                .email("not-an-email")
                .password("Password123!")
                .phoneNumber("+94771234567")
                .driverLicenseNumber("B1234567")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmailRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email", notNullValue()));

        // Test 3: Provider missing required businessName
        ProviderRegisterRequest invalidProviderRequest = ProviderRegisterRequest.builder()
                .fullName("Saman Silva")
                .email("saman@rentride.com")
                .password("Password123!")
                .phoneNumber("+94719876543")
                .businessRegistrationNumber("PV-98765")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/provider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidProviderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.businessName", notNullValue()));
    }

    @Test
    @DisplayName("Database Encryption Verification: Stored password starts with $2a$ or $2b$ and raw text is never stored")
    void testDatabaseEncryptionVerification() throws Exception {
        String rawPassword = "PlainTextPassword123!";
        String testEmail = "customer.test@rentride.com";

        CustomerRegisterRequest request = CustomerRegisterRequest.builder()
                .fullName("Test Customer")
                .email(testEmail)
                .password(rawPassword)
                .phoneNumber("+94770001122")
                .driverLicenseNumber("B0011223")
                .build();

        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Directly query the database / repository
        Optional<User> persistedUserOpt = userRepository.findByEmail(testEmail);
        assertThat(persistedUserOpt).isPresent();

        User user = persistedUserOpt.get();
        String storedPassword = user.getPassword();

        // Verify BCrypt hash format ($2a$ or $2b$)
        assertThat(storedPassword).matches("^\\$2[ab]\\$\\d{2}\\$[A-Za-z0-9./]{53}$");
        assertThat(storedPassword).startsWith("$2a$");

        // Verify the raw plain text password is NEVER stored
        assertThat(storedPassword).isNotEqualTo(rawPassword);
        assertThat(storedPassword).doesNotContain(rawPassword);
    }
}

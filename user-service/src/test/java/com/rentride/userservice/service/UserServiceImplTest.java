package com.rentride.userservice.service;

import com.rentride.userservice.dto.UpdateProfileRequest;
import com.rentride.userservice.dto.UserResponse;
import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.entity.UserStatus;
import com.rentride.userservice.exception.ResourceNotFoundException;
import com.rentride.userservice.repository.UserRepository;
import com.rentride.userservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User customerUser;
    private User providerUser;

    @BeforeEach
    void setUp() {
        customerUser = User.builder()
                .id(1L)
                .fullName("Kasun Perera")
                .email("kasun@rentride.com")
                .password("$2a$10$hashedpassword")
                .phoneNumber("+94771234567")
                .driverLicenseNumber("B1234567")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        providerUser = User.builder()
                .id(2L)
                .fullName("Nimal Fernando")
                .email("nimal@rentride.com")
                .password("$2a$10$hashedpassword")
                .phoneNumber("+94779876543")
                .businessName("Lanka Auto Rentals")
                .businessRegistrationNumber("PV123456")
                .taxIdentificationNumber("TIN987654")
                .role(Role.ROLE_PROVIDER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("getCurrentUserProfile: Returns profile when user exists")
    void testGetCurrentUserProfile_Success() {
        when(userRepository.findByEmail("kasun@rentride.com")).thenReturn(Optional.of(customerUser));

        UserResponse response = userService.getCurrentUserProfile("kasun@rentride.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("kasun@rentride.com");
        assertThat(response.getRole()).isEqualTo(Role.ROLE_CUSTOMER);
    }

    @Test
    @DisplayName("getCurrentUserProfile: Throws ResourceNotFoundException when user not found")
    void testGetCurrentUserProfile_NotFound() {
        when(userRepository.findByEmail("unknown@rentride.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUserProfile("unknown@rentride.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("unknown@rentride.com");
    }

    @Test
    @DisplayName("updateCurrentUserProfile: Updates customer name, phone, and license")
    void testUpdateCurrentUserProfile_Customer() {
        when(userRepository.findByEmail("kasun@rentride.com")).thenReturn(Optional.of(customerUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Kasun Updated")
                .phoneNumber("+94779999999")
                .driverLicenseNumber("B9999999")
                .businessName("Attempted Business") // Should not update customer
                .build();

        UserResponse response = userService.updateCurrentUserProfile("kasun@rentride.com", request);

        assertThat(response.getFullName()).isEqualTo("Kasun Updated");
        assertThat(response.getPhoneNumber()).isEqualTo("+94779999999");
        assertThat(response.getDriverLicenseNumber()).isEqualTo("B9999999");
        assertThat(customerUser.getBusinessName()).isNull();
    }

    @Test
    @DisplayName("updateCurrentUserProfile: Updates provider name, phone, and businessName")
    void testUpdateCurrentUserProfile_Provider() {
        when(userRepository.findByEmail("nimal@rentride.com")).thenReturn(Optional.of(providerUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nimal Updated")
                .phoneNumber("+94771111111")
                .businessName("New Rent Fleet")
                .driverLicenseNumber("B8888888") // Should not update provider
                .build();

        UserResponse response = userService.updateCurrentUserProfile("nimal@rentride.com", request);

        assertThat(response.getFullName()).isEqualTo("Nimal Updated");
        assertThat(response.getBusinessName()).isEqualTo("New Rent Fleet");
        assertThat(providerUser.getDriverLicenseNumber()).isNull();
    }

    @Test
    @DisplayName("getUserById: Returns user when ID exists")
    void testGetUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));

        UserResponse response = userService.getUserById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("kasun@rentride.com");
    }

    @Test
    @DisplayName("getUserById: Throws ResourceNotFoundException when ID missing")
    void testGetUserById_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getAllUsers: Returns paginated user responses")
    void testGetAllUsers_Paginated() {
        Page<User> page = new PageImpl<>(List.of(customerUser, providerUser));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<UserResponse> result = userService.getAllUsers(0, 10, Role.ROLE_CUSTOMER, UserStatus.ACTIVE);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("updateUserStatus: Successfully modifies status")
    void testUpdateUserStatus_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUserStatus(1L, UserStatus.SUSPENDED);

        assertThat(response.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(customerUser.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }
}

package com.rentride.userservice.repository;

import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Data JPA Slice tests for {@link UserRepository}.
 *
 * <p>Verifies entity persistence, constraint enforcement, and custom finder queries
 * against an embedded H2 database configured in PostgreSQL compatibility mode.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("testSaveUser_Success: Save customer and assert ID and audit timestamps generated")
    void testSaveUser_Success() {
        User customer = User.builder()
                .fullName("John Doe")
                .email("john.doe@rentride.com")
                .password("$2a$10$eACCYoNOHEqgkHgw/5B7x.3b3e8B52eM7cK6R0uYvJ5yq.6e8dDOW")
                .phoneNumber("+1234567890")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .driverLicenseNumber("DL-12345678")
                .build();

        User savedUser = userRepository.saveAndFlush(customer);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedUser.getFullName()).isEqualTo("John Doe");
        assertThat(savedUser.getEmail()).isEqualTo("john.doe@rentride.com");
        assertThat(savedUser.getRole()).isEqualTo(Role.ROLE_CUSTOMER);
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(savedUser.getDriverLicenseNumber()).isEqualTo("DL-12345678");
    }

    @Test
    @DisplayName("testFindByEmail_ReturnsUser: Persist user and assert query by email returns correct entity and role")
    void testFindByEmail_ReturnsUser() {
        User customer = User.builder()
                .fullName("Test Customer")
                .email("test.customer@rentride.com")
                .password("$2a$10$eACCYoNOHEqgkHgw/5B7x.3b3e8B52eM7cK6R0uYvJ5yq.6e8dDOW")
                .phoneNumber("+1987654321")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.saveAndFlush(customer);

        Optional<User> found = userRepository.findByEmail("test.customer@rentride.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test.customer@rentride.com");
        assertThat(found.get().getFullName()).isEqualTo("Test Customer");
        assertThat(found.get().getRole()).isEqualTo(Role.ROLE_CUSTOMER);
        assertThat(found.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("testDuplicateEmail_ThrowsException: Persisting duplicate email must throw DataIntegrityViolationException")
    void testDuplicateEmail_ThrowsException() {
        User user1 = User.builder()
                .fullName("First User")
                .email("duplicate@rentride.com")
                .password("$2a$10$hash1")
                .phoneNumber("+1111111111")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.saveAndFlush(user1);

        User user2 = User.builder()
                .fullName("Second User")
                .email("duplicate@rentride.com")
                .password("$2a$10$hash2")
                .phoneNumber("+2222222222")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }

    @Test
    @DisplayName("testFindByRole_ReturnsFilteredList: Persist 2 customers and 1 provider, assert filtered list size")
    void testFindByRole_ReturnsFilteredList() {
        User customer1 = User.builder()
                .fullName("Customer One")
                .email("customer1@rentride.com")
                .password("$2a$10$hash1")
                .phoneNumber("+1111111111")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        User customer2 = User.builder()
                .fullName("Customer Two")
                .email("customer2@rentride.com")
                .password("$2a$10$hash2")
                .phoneNumber("+2222222222")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        User provider = User.builder()
                .fullName("Provider One")
                .email("provider1@rentride.com")
                .password("$2a$10$hash3")
                .phoneNumber("+3333333333")
                .role(Role.ROLE_PROVIDER)
                .status(UserStatus.ACTIVE)
                .businessName("Fast Car Rentals Ltd")
                .businessRegistrationNumber("BRN-12345")
                .taxIdentificationNumber("TIN-67890")
                .build();

        userRepository.saveAllAndFlush(List.of(customer1, customer2, provider));

        List<User> customers = userRepository.findByRole(Role.ROLE_CUSTOMER);
        List<User> providers = userRepository.findByRole(Role.ROLE_PROVIDER);

        assertThat(customers).hasSize(2);
        assertThat(customers).extracting(User::getEmail)
                .containsExactlyInAnyOrder("customer1@rentride.com", "customer2@rentride.com");

        assertThat(providers).hasSize(1);
        assertThat(providers.get(0).getEmail()).isEqualTo("provider1@rentride.com");
        assertThat(providers.get(0).getBusinessName()).isEqualTo("Fast Car Rentals Ltd");
    }

    @Test
    @DisplayName("testFindByStatus_ReturnsFilteredList: Filter users by ACTIVE vs SUSPENDED status")
    void testFindByStatus_ReturnsFilteredList() {
        User activeUser = User.builder()
                .fullName("Active User")
                .email("active@rentride.com")
                .password("$2a$10$hash1")
                .phoneNumber("+1111111111")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        User suspendedUser = User.builder()
                .fullName("Suspended User")
                .email("suspended@rentride.com")
                .password("$2a$10$hash2")
                .phoneNumber("+2222222222")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.SUSPENDED)
                .build();

        userRepository.saveAllAndFlush(List.of(activeUser, suspendedUser));

        List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);
        List<User> suspendedUsers = userRepository.findByStatus(UserStatus.SUSPENDED);

        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getEmail()).isEqualTo("active@rentride.com");

        assertThat(suspendedUsers).hasSize(1);
        assertThat(suspendedUsers.get(0).getEmail()).isEqualTo("suspended@rentride.com");
    }

    @Test
    @DisplayName("testFindByDriverLicenseNumber_ReturnsUser: Query customer by driver license number")
    void testFindByDriverLicenseNumber_ReturnsUser() {
        User customer = User.builder()
                .fullName("Licensed Driver")
                .email("driver@rentride.com")
                .password("$2a$10$hash")
                .phoneNumber("+1234567890")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .driverLicenseNumber("DL-XYZ-999")
                .build();
        userRepository.saveAndFlush(customer);

        Optional<User> found = userRepository.findByDriverLicenseNumber("DL-XYZ-999");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("driver@rentride.com");

        Optional<User> notFound = userRepository.findByDriverLicenseNumber("NON_EXISTENT");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("testExistsByEmail: Return true when user exists, false otherwise")
    void testExistsByEmail() {
        User user = User.builder()
                .fullName("Existing User")
                .email("exists@rentride.com")
                .password("$2a$10$hash")
                .phoneNumber("+1234567890")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.saveAndFlush(user);

        assertThat(userRepository.existsByEmail("exists@rentride.com")).isTrue();
        assertThat(userRepository.existsByEmail("missing@rentride.com")).isFalse();
    }
}

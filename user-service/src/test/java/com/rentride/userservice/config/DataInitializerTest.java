package com.rentride.userservice.config;

import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DataInitializer}.
 *
 * <p>Verifies baseline admin account seeding logic and idempotent behavior.
 */
@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    @DisplayName("Should seed baseline admin user when no admin exists in database")
    void shouldSeedAdminWhenNoneExists() {
        when(userRepository.existsByEmail("admin@rentride.com")).thenReturn(false);
        when(userRepository.findByRole(Role.ROLE_ADMIN)).thenReturn(Collections.emptyList());

        dataInitializer.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(captor.capture());

        User seededAdmin = captor.getValue();
        assertThat(seededAdmin.getEmail()).isEqualTo("admin@rentride.com");
        assertThat(seededAdmin.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(seededAdmin.getPassword()).isNotBlank();
        assertThat(seededAdmin.getFullName()).isEqualTo("RentRide Administrator");
    }

    @Test
    @DisplayName("Should skip seeding when admin with email already exists")
    void shouldSkipSeedingWhenAdminEmailExists() {
        when(userRepository.existsByEmail("admin@rentride.com")).thenReturn(true);

        dataInitializer.run();

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should skip seeding when another admin user exists by role")
    void shouldSkipSeedingWhenAdminRoleExists() {
        when(userRepository.existsByEmail("admin@rentride.com")).thenReturn(false);
        User existingAdmin = User.builder()
                .email("custom.admin@rentride.com")
                .role(Role.ROLE_ADMIN)
                .build();
        when(userRepository.findByRole(Role.ROLE_ADMIN)).thenReturn(List.of(existingAdmin));

        dataInitializer.run();

        verify(userRepository, never()).save(any(User.class));
    }
}

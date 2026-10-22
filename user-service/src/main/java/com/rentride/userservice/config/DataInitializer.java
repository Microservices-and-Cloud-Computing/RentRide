package com.rentride.userservice.config;

import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.entity.UserStatus;
import com.rentride.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Baseline data seeder for development and local runtime environments.
 * Seeds a default platform administrator account if no admin user is present.
 * Disabled under the 'test' profile to maintain test isolation.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        log.info("Checking baseline data initialization for RentRide user-service...");
        seedAdminUser();
    }

    private void seedAdminUser() {
        String adminEmail = "admin@rentride.com";

        // Check if the baseline admin or any admin already exists
        boolean adminExists = Boolean.TRUE.equals(userRepository.existsByEmail(adminEmail))
                || !userRepository.findByRole(Role.ROLE_ADMIN).isEmpty();

        if (adminExists) {
            log.info("Administrator account already exists in database. Skipping seed.");
            return;
        }

        log.info("No administrator found. Seeding baseline administrator: {}", adminEmail);

        // Pre-computed BCrypt hash for default initial password 'Admin@123'
        // $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
        User admin = User.builder()
                .fullName("RentRide Administrator")
                .email(adminEmail)
                .password("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy")
                .phoneNumber("+1000000000")
                .role(Role.ROLE_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(admin);
        log.info("Successfully seeded baseline administrator account with email={}", adminEmail);
    }
}

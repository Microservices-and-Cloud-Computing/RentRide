package com.rentride.userservice.service.impl;

import com.rentride.userservice.dto.UpdateProfileRequest;
import com.rentride.userservice.dto.UserResponse;
import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.entity.UserStatus;
import com.rentride.userservice.exception.ResourceNotFoundException;
import com.rentride.userservice.repository.UserRepository;
import com.rentride.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link UserService} managing user profile lookups,
 * updates under role constraints, and administrative user management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse getCurrentUserProfile(String email) {
        log.info("Fetching user profile for email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUserProfile(String email, UpdateProfileRequest request) {
        log.info("Updating profile for email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Update core identity attributes
        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());

        // Role-constrained updates
        if (user.getRole() == Role.ROLE_PROVIDER && request.getBusinessName() != null) {
            user.setBusinessName(request.getBusinessName().trim());
        }
        if (user.getRole() == Role.ROLE_CUSTOMER && request.getDriverLicenseNumber() != null) {
            user.setDriverLicenseNumber(request.getDriverLicenseNumber().trim());
        }

        User updatedUser = userRepository.save(user);
        log.info("Updated profile for user id: {}, email: {}", updatedUser.getId(), updatedUser.getEmail());
        return UserResponse.fromEntity(updatedUser);
    }

    @Override
    public UserResponse getUserById(Long userId) {
        log.info("Fetching user by id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return UserResponse.fromEntity(user);
    }

    @Override
    public Page<UserResponse> getAllUsers(int page, int size, Role role, UserStatus status) {
        log.info("Admin query for users: page={}, size={}, role={}, status={}", page, size, role, status);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        Specification<User> spec = Specification.where(null);
        if (role != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        Page<User> userPage = userRepository.findAll(spec, pageable);
        return userPage.map(UserResponse::fromEntity);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long userId, UserStatus newStatus) {
        log.info("Admin updating status for user id: {} to {}", userId, newStatus);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setStatus(newStatus);
        User updated = userRepository.save(user);
        log.info("User id: {} status updated to {}", updated.getId(), updated.getStatus());
        return UserResponse.fromEntity(updated);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return Boolean.TRUE.equals(userRepository.existsByEmail(email));
    }

    @Override
    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Override
    public List<User> findByStatus(UserStatus status) {
        return userRepository.findByStatus(status);
    }

    @Override
    public Optional<User> findByDriverLicenseNumber(String driverLicenseNumber) {
        return userRepository.findByDriverLicenseNumber(driverLicenseNumber);
    }

    @Override
    @Transactional
    public User save(User user) {
        if (Boolean.TRUE.equals(userRepository.existsByEmail(user.getEmail()))) {
            log.warn("Attempt to register duplicate email={}", user.getEmail());
            throw new IllegalStateException(
                    "A user with email '" + user.getEmail() + "' already exists.");
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}

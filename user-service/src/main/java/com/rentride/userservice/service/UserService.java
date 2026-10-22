package com.rentride.userservice.service;

import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.entity.UserStatus;
import com.rentride.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Business-logic layer for user management in the RentRide platform.
 *
 * <p>Coordinates between REST controllers, security handlers, and persistence layer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * Retrieves all users persisted in the system.
     *
     * @return list of all {@link User} records
     */
    public List<User> findAll() {
        log.debug("Fetching all users");
        return userRepository.findAll();
    }

    /**
     * Looks up a user by their primary key.
     *
     * @param id the user's database ID
     * @return an {@link Optional} containing the user if found; empty otherwise
     */
    public Optional<User> findById(Long id) {
        log.debug("Fetching user by id={}", id);
        return userRepository.findById(id);
    }

    /**
     * Looks up a user by their email address.
     *
     * @param email the user's email address
     * @return an {@link Optional} containing the user if found; empty otherwise
     */
    public Optional<User> findByEmail(String email) {
        log.debug("Fetching user by email={}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Checks if a user exists with the given email address.
     *
     * @param email the email address to check
     * @return true if exists, false otherwise
     */
    public boolean existsByEmail(String email) {
        return Boolean.TRUE.equals(userRepository.existsByEmail(email));
    }

    /**
     * Retrieves users filtered by their platform role.
     *
     * @param role the {@link Role} to filter by
     * @return list of matching users
     */
    public List<User> findByRole(Role role) {
        log.debug("Fetching users by role={}", role);
        return userRepository.findByRole(role);
    }

    /**
     * Retrieves users filtered by their account status.
     *
     * @param status the {@link UserStatus} to filter by
     * @return list of matching users
     */
    public List<User> findByStatus(UserStatus status) {
        log.debug("Fetching users by status={}", status);
        return userRepository.findByStatus(status);
    }

    /**
     * Looks up a user by driver's license number.
     *
     * @param driverLicenseNumber the driver license number
     * @return an {@link Optional} containing the user if found; empty otherwise
     */
    public Optional<User> findByDriverLicenseNumber(String driverLicenseNumber) {
        log.debug("Fetching user by driverLicenseNumber={}", driverLicenseNumber);
        return userRepository.findByDriverLicenseNumber(driverLicenseNumber);
    }

    /**
     * Persists a new {@link User} to the database.
     *
     * @param user the user entity to save
     * @return the saved entity with database-assigned ID
     * @throws IllegalStateException if a user with the same email already exists
     */
    @Transactional
    public User save(User user) {
        if (Boolean.TRUE.equals(userRepository.existsByEmail(user.getEmail()))) {
            log.warn("Attempt to register duplicate email={}", user.getEmail());
            throw new IllegalStateException(
                    "A user with email '" + user.getEmail() + "' already exists.");
        }
        User saved = userRepository.save(user);
        log.info("Created new user id={}, email={}, role={}", saved.getId(), saved.getEmail(), saved.getRole());
        return saved;
    }

    /**
     * Removes a user from the system by their primary key.
     *
     * @param id the ID of the user to delete
     */
    @Transactional
    public void deleteById(Long id) {
        log.info("Deleting user id={}", id);
        userRepository.deleteById(id);
    }
}

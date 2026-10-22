package com.rentride.userservice.service;

import com.rentride.userservice.entity.User;
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
 * <p>This service acts as the single point of coordination between the REST
 * controllers and the persistence layer ({@link UserRepository}).  All database
 * interactions are wrapped in transactions via {@link Transactional}.
 *
 * <p><b>Future growth:</b> Authentication (password hashing, token generation)
 * and role-based access checks will be added here in subsequent commits.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)  // Default read-only; write methods override with @Transactional
public class UserService {

    private final UserRepository userRepository;

    /**
     * Retrieves all users persisted in the system.
     * Intended for admin use only; pagination support will be added in a later commit.
     *
     * @return an unmodifiable list of all {@link User} records
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
     * Looks up a user by their e-mail address.
     * Used by the authentication flow to locate an account before validating credentials.
     *
     * @param email the user's e-mail address
     * @return an {@link Optional} containing the user if found; empty otherwise
     */
    public Optional<User> findByEmail(String email) {
        log.debug("Fetching user by email={}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Persists a new {@link User} to the database.
     *
     * <p>Callers are responsible for hashing the password before passing the entity
     * to this method.  The service does <em>not</em> perform encoding here to keep
     * it decoupled from the chosen hashing strategy.
     *
     * @param user the fully-populated (and validated) user entity to save
     * @return the saved entity with a database-assigned {@code id}
     * @throws IllegalStateException if a user with the same e-mail already exists
     */
    @Transactional
    public User save(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
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

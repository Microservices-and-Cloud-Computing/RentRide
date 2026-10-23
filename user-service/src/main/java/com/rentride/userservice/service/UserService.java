package com.rentride.userservice.service;

import com.rentride.userservice.dto.UpdateProfileRequest;
import com.rentride.userservice.dto.UserResponse;
import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.entity.UserStatus;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

/**
 * Service contract defining user profile management, administrative queries,
 * role-based access control workflows, and persistence queries.
 */
public interface UserService {

    /**
     * Retrieves the profile of the currently authenticated user by their email address.
     *
     * @param email authenticated user's email
     * @return {@link UserResponse} profile details
     */
    UserResponse getCurrentUserProfile(String email);

    /**
     * Updates the authenticated user's editable profile fields according to role constraints.
     *
     * @param email   authenticated user's email
     * @param request profile fields to update
     * @return updated {@link UserResponse}
     */
    UserResponse updateCurrentUserProfile(String email, UpdateProfileRequest request);

    /**
     * Looks up user profile details by database ID.
     * Accessible by the user themselves or administrators.
     *
     * @param userId target user ID
     * @return {@link UserResponse}
     */
    UserResponse getUserById(Long userId);

    /**
     * Admin-only query returning a paginated list of users with optional filtering.
     *
     * @param page   zero-based page index
     * @param size   number of records per page
     * @param role   optional role filter (null for all)
     * @param status optional status filter (null for all)
     * @return {@link Page} of {@link UserResponse}
     */
    Page<UserResponse> getAllUsers(int page, int size, Role role, UserStatus status);

    /**
     * Admin-only operation to activate, suspend, or update a user's account status.
     *
     * @param userId    target user ID
     * @param newStatus new {@link UserStatus} to apply
     * @return updated {@link UserResponse}
     */
    UserResponse updateUserStatus(Long userId, UserStatus newStatus);

    /**
     * Retrieves all users persisted in the system.
     *
     * @return list of all {@link User} entities
     */
    List<User> findAll();

    /**
     * Looks up a user entity by primary key.
     *
     * @param id the user ID
     * @return optional containing the user if found
     */
    Optional<User> findById(Long id);

    /**
     * Looks up a user entity by email.
     *
     * @param email the user email
     * @return optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists with the given email address.
     *
     * @param email the email address
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Retrieves users matching the specified role.
     *
     * @param role the role to filter by
     * @return list of users
     */
    List<User> findByRole(Role role);

    /**
     * Retrieves users matching the specified status.
     *
     * @param status the status to filter by
     * @return list of users
     */
    List<User> findByStatus(UserStatus status);

    /**
     * Looks up a user by driver license number.
     *
     * @param driverLicenseNumber driver license identifier
     * @return optional containing user if found
     */
    Optional<User> findByDriverLicenseNumber(String driverLicenseNumber);

    /**
     * Persists a new user entity.
     *
     * @param user user entity to save
     * @return saved entity
     */
    User save(User user);

    /**
     * Deletes a user by primary key.
     *
     * @param id user ID
     */
    void deleteById(Long id);
}

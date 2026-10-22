package com.rentride.userservice.repository;

import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entity persistence operations.
 *
 * <p>Provides full CRUD operations and lookup methods mapped to schema indexes.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Looks up a user by their unique email address.
     *
     * @param email the email address to look up
     * @return an {@link Optional} containing the user if found, or empty if not
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given email address already exists.
     *
     * @param email the email address to check
     * @return {@code true} if a record exists with the email, otherwise {@code false}
     */
    Boolean existsByEmail(String email);

    /**
     * Retrieves all users matching the specified platform role.
     *
     * @param role the {@link Role} to filter by
     * @return list of matching users
     */
    List<User> findByRole(Role role);

    /**
     * Retrieves all users matching the specified account status.
     *
     * @param status the {@link UserStatus} to filter by
     * @return list of matching users
     */
    List<User> findByStatus(UserStatus status);

    /**
     * Looks up a customer by their driver's license number.
     *
     * @param driverLicenseNumber the driver's license identifier
     * @return an {@link Optional} containing the user if found, or empty if not
     */
    Optional<User> findByDriverLicenseNumber(String driverLicenseNumber);
}

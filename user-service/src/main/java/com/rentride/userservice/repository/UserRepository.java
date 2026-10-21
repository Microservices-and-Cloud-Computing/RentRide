package com.rentride.userservice.repository;

import com.rentride.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entity persistence operations.
 *
 * <p>Extends {@link JpaRepository} to inherit the full CRUD method set, plus
 * pagination and sorting support, with no boilerplate required.
 *
 * <p>Custom finders are declared here as query-method signatures; Spring Data
 * automatically derives the JPQL at application startup.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Looks up a user by their unique e-mail address.
     * Used by the authentication layer during login and duplicate-check on registration.
     *
     * @param email the e-mail address to search for (case-sensitive in PostgreSQL by default)
     * @return an {@link Optional} containing the matching {@link User}, or empty if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given e-mail address already exists.
     * Preferred over {@link #findByEmail(String)} when the caller only needs a boolean
     * answer (avoids fetching the full entity).
     *
     * @param email the e-mail address to test
     * @return {@code true} if a user with that e-mail exists; {@code false} otherwise
     */
    boolean existsByEmail(String email);
}

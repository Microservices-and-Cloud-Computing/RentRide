package com.rentride.vehicleservice.repository;

import com.rentride.vehicleservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Category} entities.
 *
 * <p>Provides CRUD operations and lookup methods for vehicle classifications.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Finds a category by its unique classification name.
     *
     * @param name unique name of the category (e.g., "SEDAN", "SUV")
     * @return Optional containing the matching Category if found
     */
    Optional<Category> findByName(String name);

    /**
     * Checks if a category with the specified name already exists.
     *
     * @param name classification name to check
     * @return true if a category exists with the given name, false otherwise
     */
    boolean existsByName(String name);
}

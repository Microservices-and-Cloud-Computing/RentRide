package com.rentride.vehicleservice.repository;

import com.rentride.vehicleservice.entity.Vehicle;
import com.rentride.vehicleservice.entity.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Vehicle} inventory entities.
 *
 * <p>Provides fleet query capabilities, status filtering, geographic availability lookups,
 * dynamic specification execution with eager category fetching, and uniqueness validation.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    /**
     * Executes dynamic specification search query, eagerly loading the associated {@code Category}
     * via an entity graph to eliminate N+1 select queries during pagination.
     */
    @Override
    @EntityGraph(attributePaths = {"category"})
    Page<Vehicle> findAll(@Nullable Specification<Vehicle> spec, Pageable pageable);

    /**
     * Retrieves all vehicles registered under a specific rental provider.
     *
     * @param providerId unique ID of the rental provider
     * @return list of vehicles owned by the provider
     */
    List<Vehicle> findByProviderId(Long providerId);

    /**
     * Retrieves all vehicles filtered by their operational status.
     *
     * @param status vehicle status (e.g., AVAILABLE, RENTED)
     * @return list of vehicles with the specified status
     */
    List<Vehicle> findByStatus(VehicleStatus status);

    /**
     * Retrieves all vehicles associated with a given category ID.
     *
     * @param categoryId ID of the parent vehicle category
     * @return list of vehicles belonging to the category
     */
    List<Vehicle> findByCategoryId(Long categoryId);

    /**
     * Retrieves available vehicles within a specific city or geographic location,
     * matching the location case-insensitively.
     *
     * @param location city or dispatch branch name
     * @param status operational status (typically AVAILABLE)
     * @return list of matching vehicles
     */
    List<Vehicle> findByLocationIgnoreCaseAndStatus(String location, VehicleStatus status);

    /**
     * Finds a vehicle by its unique license plate number.
     *
     * @param licensePlateNumber vehicle registration plate number
     * @return Optional containing the matching Vehicle if found
     */
    Optional<Vehicle> findByLicensePlateNumber(String licensePlateNumber);

    /**
     * Checks if a vehicle with the given license plate number already exists.
     *
     * @param licensePlateNumber vehicle registration plate number to check
     * @return true if a vehicle exists with the plate number, false otherwise
     */
    boolean existsByLicensePlateNumber(String licensePlateNumber);
}

package com.rentride.vehicleservice.specification;

import com.rentride.vehicleservice.dto.VehicleSearchCriteria;
import com.rentride.vehicleservice.entity.Vehicle;
import com.rentride.vehicleservice.entity.VehicleStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA Specification for filtering vehicles by category, location, pricing,
 * mechanical specifications, and availability.
 */
public final class VehicleSpecification {

    private VehicleSpecification() {
        // Private constructor for utility class
    }

    /**
     * Builds a composite JPA {@link Specification} based on customer search criteria.
     * Enforces that only vehicles with {@link VehicleStatus#AVAILABLE} status are returned.
     *
     * @param criteria customer search and filtering criteria
     * @return composite Specification
     */
    public static Specification<Vehicle> build(VehicleSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Strictly restrict to AVAILABLE vehicles for customer search/discovery
            predicates.add(cb.equal(root.get("status"), VehicleStatus.AVAILABLE));

            if (criteria != null) {
                // Category filter
                if (criteria.getCategoryId() != null) {
                    predicates.add(cb.equal(root.get("category").get("id"), criteria.getCategoryId()));
                }

                // Location filter (case-insensitive substring match)
                if (criteria.getLocation() != null && !criteria.getLocation().trim().isEmpty()) {
                    predicates.add(cb.like(
                            cb.lower(root.get("location")),
                            "%" + criteria.getLocation().trim().toLowerCase() + "%"
                    ));
                }

                // Price range filters (inclusive)
                if (criteria.getMinPrice() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("dailyRentalRate"), criteria.getMinPrice()));
                }

                if (criteria.getMaxPrice() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("dailyRentalRate"), criteria.getMaxPrice()));
                }

                // Fuel type filter
                if (criteria.getFuelType() != null) {
                    predicates.add(cb.equal(root.get("fuelType"), criteria.getFuelType()));
                }

                // Transmission filter
                if (criteria.getTransmission() != null) {
                    predicates.add(cb.equal(root.get("transmission"), criteria.getTransmission()));
                }

                // Minimum seating capacity filter (>= minSeats)
                if (criteria.getMinSeats() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("seatingCapacity"), criteria.getMinSeats()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

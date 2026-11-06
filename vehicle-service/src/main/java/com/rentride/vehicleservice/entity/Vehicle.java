package com.rentride.vehicleservice.entity;

import com.rentride.vehicleservice.exception.InvalidStateTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * JPA entity representing a vehicle available in the RentRide platform catalog.
 *
 * <p>Contains core vehicle attributes, rental provider ownership reference,
 * mechanical specifications, daily pricing, and live availability status.
 * Maps to table {@code vehicles} in the microservice database.
 */
@Entity
@Table(
        name = "vehicles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vehicles_license_plate_number", columnNames = "license_plate_number")
        },
        indexes = {
                @Index(name = "idx_vehicles_provider_id", columnList = "provider_id"),
                @Index(name = "idx_vehicles_license_plate_number", columnList = "license_plate_number"),
                @Index(name = "idx_vehicles_status", columnList = "status"),
                @Index(name = "idx_vehicles_location_status", columnList = "location, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "category")
@EqualsAndHashCode(callSuper = false, exclude = "category")
public class Vehicle extends BaseEntity {

    // ── Primary Key ──────────────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    // ── Provider Ownership ───────────────────────────────────────────────────
    @NotNull(message = "Provider ID is required")
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    // ── Vehicle Specifications ───────────────────────────────────────────────
    @NotBlank(message = "Make is required")
    @Size(max = 100, message = "Make must not exceed 100 characters")
    @Column(name = "make", nullable = false, length = 100)
    private String make;

    @NotBlank(message = "Model is required")
    @Size(max = 100, message = "Model must not exceed 100 characters")
    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @NotNull(message = "Manufacturing year is required")
    @Column(name = "manufacturing_year", nullable = false)
    private Integer manufacturingYear;

    @NotBlank(message = "License plate number is required")
    @Size(max = 50, message = "License plate number must not exceed 50 characters")
    @Column(name = "license_plate_number", nullable = false, unique = true, length = 50)
    private String licensePlateNumber;

    @NotNull(message = "Seating capacity is required")
    @Column(name = "seating_capacity", nullable = false)
    private Integer seatingCapacity;

    @NotNull(message = "Fuel type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 30)
    private FuelType fuelType;

    @NotNull(message = "Transmission type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "transmission", nullable = false, length = 30)
    private TransmissionType transmission;

    // ── Pricing and Status ───────────────────────────────────────────────────
    @NotNull(message = "Daily rental rate is required")
    @Column(name = "daily_rental_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRentalRate;

    @NotNull(message = "Vehicle status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    // ── Location & Media ─────────────────────────────────────────────────────
    @NotBlank(message = "Location is required")
    @Size(max = 150, message = "Location must not exceed 150 characters")
    @Column(name = "location", nullable = false, length = 150)
    private String location;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // ── Category Association ─────────────────────────────────────────────────
    @NotNull(message = "Category is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vehicles_category_id"))
    private Category category;

    // ── Domain Logic & Lifecycle Transitions ─────────────────────────────────

    /**
     * Validates and applies an operational status transition according to RentRide lifecycle rules.
     *
     * <p>Permissible state transitions:
     * <ul>
     *   <li>{@link VehicleStatus#AVAILABLE} &rarr; {@link VehicleStatus#RENTED} (booking confirmed/locked)</li>
     *   <li>{@link VehicleStatus#RENTED} &rarr; {@link VehicleStatus#AVAILABLE} (vehicle returned in good condition)</li>
     *   <li>{@link VehicleStatus#RENTED} &rarr; {@link VehicleStatus#MAINTENANCE} (vehicle returned needing repair/servicing)</li>
     *   <li>{@link VehicleStatus#MAINTENANCE} &rarr; {@link VehicleStatus#AVAILABLE} (servicing completed and inspection cleared)</li>
     * </ul>
     *
     * @param targetStatus the desired target VehicleStatus
     * @throws InvalidStateTransitionException if the status transition is not permitted
     */
    public void transitionTo(VehicleStatus targetStatus) {
        if (targetStatus == null) {
            throw new InvalidStateTransitionException("Target vehicle status cannot be null");
        }
        if (!isValidTransition(this.status, targetStatus)) {
            throw new InvalidStateTransitionException(
                    String.format("Vehicle cannot transition from %s to %s", this.status, targetStatus)
            );
        }
        this.status = targetStatus;
    }

    /**
     * Determines whether transitioning from the {@code current} status to the {@code target} status is valid.
     *
     * @param current current vehicle status
     * @param target target vehicle status
     * @return true if the transition is allowed by business rules, false otherwise
     */
    public static boolean isValidTransition(VehicleStatus current, VehicleStatus target) {
        if (current == null || target == null || current == target) {
            return false;
        }
        return switch (current) {
            case AVAILABLE -> target == VehicleStatus.RENTED;
            case RENTED -> target == VehicleStatus.AVAILABLE || target == VehicleStatus.MAINTENANCE;
            case MAINTENANCE -> target == VehicleStatus.AVAILABLE;
            case DECOMMISSIONED -> false;
        };
    }

    /**
     * Evaluates whether this vehicle is currently ready and eligible for customer booking dispatch.
     *
     * @return true if the current status is AVAILABLE, false otherwise
     */
    public boolean isAvailableForBooking() {
        return this.status == VehicleStatus.AVAILABLE;
    }
}

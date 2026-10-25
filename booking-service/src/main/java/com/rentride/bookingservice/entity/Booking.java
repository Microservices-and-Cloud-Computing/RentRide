package com.rentride.bookingservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity representing a customer vehicle reservation agreement in the RentRide platform.
 *
 * <p>Tracks reservation lifecycle states, rental duration, daily rates, calculated costs,
 * provider ownership, and customer association. Maps to table {@code bookings}.
 */
@Entity
@Table(
        name = "bookings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bookings_booking_reference", columnNames = "booking_reference")
        },
        indexes = {
                @Index(name = "idx_bookings_booking_reference", columnList = "booking_reference"),
                @Index(name = "idx_bookings_customer_id", columnList = "customer_id"),
                @Index(name = "idx_bookings_provider_id", columnList = "provider_id"),
                @Index(name = "idx_bookings_vehicle_id", columnList = "vehicle_id"),
                @Index(name = "idx_bookings_vehicle_status_dates", columnList = "vehicle_id, status, start_date, end_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(callSuper = false)
public class Booking extends BaseEntity {

    /**
     * Primary surrogate key. Auto-incremented sequence identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Globally unique business reservation reference (e.g., RR-BK-20261025-A1B2C3).
     */
    @NotNull
    @Column(name = "booking_reference", nullable = false, unique = true, length = 64)
    private String bookingReference;

    /**
     * Foreign reference ID of the customer who booked the vehicle.
     */
    @NotNull
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /**
     * Foreign reference ID of the rental provider fleet owner.
     */
    @NotNull
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    /**
     * Foreign reference ID of the reserved vehicle.
     */
    @NotNull
    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    /**
     * Rental reservation start date (inclusive).
     */
    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Rental reservation end date (inclusive).
     */
    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Total calculated rental duration in calendar days.
     */
    @NotNull
    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    /**
     * Daily rental rate applied at time of booking creation.
     */
    @NotNull
    @Column(name = "daily_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRate;

    /**
     * Total cost of the rental reservation (dailyRate * totalDays).
     */
    @NotNull
    @Column(name = "total_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCost;

    /**
     * Current lifecycle status of the reservation.
     */
    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BookingStatus status = BookingStatus.PENDING_PAYMENT;

    /**
     * Operational pickup remarks (e.g., condition checks, odometer reading).
     */
    @Column(name = "pickup_notes", length = 500)
    private String pickupNotes;

    /**
     * Operational return remarks (e.g., fuel level, damage notes).
     */
    @Column(name = "return_notes", length = 500)
    private String returnNotes;

    /**
     * Actual timestamp when the vehicle was handed back by the customer.
     */
    @Column(name = "actual_return_date")
    private LocalDateTime actualReturnDate;
}


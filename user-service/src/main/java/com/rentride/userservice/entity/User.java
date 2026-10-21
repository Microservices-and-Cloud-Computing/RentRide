package com.rentride.userservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * JPA entity representing a platform user in the RentRide system.
 *
 * <p>A {@code User} can play one of three roles on the platform:
 * <ul>
 *   <li>{@link Role#CUSTOMER}         — Books vehicles.</li>
 *   <li>{@link Role#RENTAL_PROVIDER}  — Lists and manages vehicles.</li>
 *   <li>{@link Role#ADMIN}            — Platform administrator with full access.</li>
 * </ul>
 *
 * <p>Mapped to the {@code users} table in {@code rentride_user_db}.
 * Schema is managed via {@code spring.jpa.hibernate.ddl-auto=update}.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "passwordHash")  // Never log the password hash
public class User {

    // ── Primary Key ────────────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    // ── Identity Fields ───────────────────────────────────────────────────
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid e-mail address")
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    // ── Role / Status ──────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.CUSTOMER;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    // ── Audit Timestamps ───────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ── Role Enum (stored as VARCHAR for readability) ──────────────────────
    public enum Role {
        CUSTOMER,
        RENTAL_PROVIDER,
        ADMIN
    }
}

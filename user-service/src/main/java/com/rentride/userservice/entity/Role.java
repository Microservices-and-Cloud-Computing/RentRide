package com.rentride.userservice.entity;

/**
 * Enumeration representing user authorization roles within the RentRide platform.
 *
 * <ul>
 *   <li>{@link #ROLE_CUSTOMER} — Standard platform customer who books vehicles.</li>
 *   <li>{@link #ROLE_PROVIDER} — Vehicle rental provider who manages and leases fleet vehicles.</li>
 *   <li>{@link #ROLE_ADMIN}    — Platform administrator with elevated permissions across all services.</li>
 * </ul>
 */
public enum Role {
    ROLE_CUSTOMER,
    ROLE_PROVIDER,
    ROLE_ADMIN
}

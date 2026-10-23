package com.rentride.userservice.entity;

/**
 * Enumeration representing the account lifecycle status of a platform user.
 *
 * <ul>
 *   <li>{@link #ACTIVE}               — User account is active and fully functional.</li>
 *   <li>{@link #SUSPENDED}            — User account has been administratively disabled.</li>
 *   <li>{@link #PENDING_VERIFICATION} — User has registered but awaits email/identity verification.</li>
 * </ul>
 */
public enum UserStatus {
    ACTIVE,
    SUSPENDED,
    PENDING_VERIFICATION
}

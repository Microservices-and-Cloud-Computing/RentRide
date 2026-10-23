package com.rentride.userservice.security;

import com.rentride.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Security expression helper for fine-grained authorization checks.
 * Determines whether the authenticated principal is an administrator or the owner of a specified user ID.
 */
@Component("userSecurity")
@RequiredArgsConstructor
public class UserSecurity {

    private final UserRepository userRepository;

    /**
     * Checks if the authenticated principal owns the specified ID or has ROLE_ADMIN.
     *
     * @param id             target user database ID
     * @param authentication current security context authentication
     * @return {@code true} if authorized; {@code false} otherwise
     */
    public boolean isCurrentUserOrAdmin(Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            return true;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal up) {
            return Objects.equals(up.getId(), id);
        }
        if (principal instanceof UserDetails ud) {
            return userRepository.findByEmail(ud.getUsername())
                    .map(u -> Objects.equals(u.getId(), id))
                    .orElse(false);
        }
        return false;
    }
}

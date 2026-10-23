package com.rentride.userservice.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom {@link UserDetails} implementation encapsulating user identity,
 * primary key ID, email, role, and granted authorities for method-level security evaluations.
 */
@Getter
@AllArgsConstructor
@Builder
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;

    @JsonIgnore
    private final String password;

    private final Role role;
    private final UserStatus status;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Factory method creating a {@link UserPrincipal} from a {@link User} entity.
     *
     * @param user the persistent user entity
     * @return populated {@link UserPrincipal}
     */
    public static UserPrincipal create(User user) {
        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .role(user.getRole())
                .status(user.getStatus())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())))
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}

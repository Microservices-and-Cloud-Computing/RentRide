package com.rentride.userservice.security;

import com.rentride.userservice.entity.Role;
import com.rentride.userservice.entity.User;
import com.rentride.userservice.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secretKey = "NGRmYmE1ZjQ4M2E4ZjI4ZjM2YTkzMjQ5OTM2NWNlMWQ3OGFmOWY4ZDgyZGMzNzRmOWI0OTFhZmQ=";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L); // 24 hours
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L); // 7 days
    }

    @Test
    @DisplayName("generateToken: Should embed userId, email, role, status in claims")
    void testGenerateToken_ClaimsEmbedded() {
        User user = User.builder()
                .id(101L)
                .email("test.user@rentride.com")
                .role(Role.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("test.user@rentride.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_CUSTOMER");
        assertThat(jwtService.extractUserId(token)).isEqualTo(101L);
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("generateRefreshToken: Should issue valid token with expiration around 7 days")
    void testGenerateRefreshToken() {
        User user = User.builder()
                .id(102L)
                .email("refresh.user@rentride.com")
                .role(Role.ROLE_PROVIDER)
                .status(UserStatus.ACTIVE)
                .build();

        String refreshToken = jwtService.generateRefreshToken(user);

        assertThat(refreshToken).isNotBlank();
        assertThat(jwtService.extractEmail(refreshToken)).isEqualTo("refresh.user@rentride.com");
        assertThat(jwtService.isTokenExpired(refreshToken)).isFalse();

        Date expiration = jwtService.extractExpiration(refreshToken);
        long diffHours = (expiration.getTime() - System.currentTimeMillis()) / (1000 * 60 * 60);
        assertThat(diffHours).isBetween(160L, 169L); // ~7 days = 168 hours
    }

    @Test
    @DisplayName("isTokenValid: Should return true for matching UserDetails and false for mismatched email")
    void testIsTokenValid_WithUserDetails() {
        User user = User.builder()
                .id(103L)
                .email("valid@rentride.com")
                .role(Role.ROLE_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        String token = jwtService.generateToken(user);

        UserDetails matchingUserDetails = new org.springframework.security.core.userdetails.User(
                "valid@rentride.com",
                "password",
                Collections.emptyList()
        );

        UserDetails mismatchedUserDetails = new org.springframework.security.core.userdetails.User(
                "other@rentride.com",
                "password",
                Collections.emptyList()
        );

        assertThat(jwtService.isTokenValid(token, matchingUserDetails)).isTrue();
        assertThat(jwtService.isTokenValid(token, mismatchedUserDetails)).isFalse();
    }
}

package com.rentride.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private static final String TEST_SECRET = "NGRmYmE1ZjQ4M2E4ZjI4ZjM2YTkzMjQ5OTM2NWNlMWQ3OGFmOWY4ZDgyZGMzNzRmOWI0OTFhZmQ=";

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secretKey", TEST_SECRET);
    }

    private String createToken(String subject, Long userId, String role, long expirationOffsetMs) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        return Jwts.builder()
                .subject(subject)
                .claims(Map.of("userId", userId, "role", role))
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationOffsetMs))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("validateToken: Valid signature and unexpired token returns true")
    void testValidateToken_Valid_ReturnsTrue() {
        String token = createToken("kasun@rentride.com", 1L, "ROLE_CUSTOMER", 60000);

        boolean isValid = jwtUtils.validateToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("getClaims: Extracts userId, subject (email), and role accurately")
    void testGetClaims_Success() {
        String token = createToken("kasun@rentride.com", 42L, "ROLE_ADMIN", 60000);

        Claims claims = jwtUtils.getClaims(token);

        assertThat(claims.getSubject()).isEqualTo("kasun@rentride.com");
        assertThat(claims.get("userId", Long.class)).isEqualTo(42L);
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("validateToken: Expired token returns false")
    void testValidateToken_Expired_ReturnsFalse() {
        String expiredToken = createToken("kasun@rentride.com", 1L, "ROLE_CUSTOMER", -60000);

        boolean isValid = jwtUtils.validateToken(expiredToken);

        assertThat(isValid).isFalse();
        assertThat(jwtUtils.isTokenExpired(expiredToken)).isTrue();
    }

    @Test
    @DisplayName("validateToken: Tampered token returns false")
    void testValidateToken_Tampered_ReturnsFalse() {
        String token = createToken("kasun@rentride.com", 1L, "ROLE_CUSTOMER", 60000);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        boolean isValid = jwtUtils.validateToken(tamperedToken);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("validateToken: Malformed string or null returns false")
    void testValidateToken_MalformedOrNull_ReturnsFalse() {
        assertThat(jwtUtils.validateToken("not.a.valid.jwt")).isFalse();
        assertThat(jwtUtils.validateToken("")).isFalse();
        assertThat(jwtUtils.validateToken(null)).isFalse();
    }
}

package com.rentride.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utility for parsing, validating, and extracting claims from JSON Web Tokens (JWT)
 * at the API Gateway level.
 */
@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    /**
     * Reconstructs the cryptographic signing key from the Base64-encoded secret.
     *
     * @return {@link SecretKey} suitable for HMAC-SHA verification
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Parses the given JWT token and extracts its claims payload.
     *
     * @param token raw JWT token string
     * @return {@link Claims} instance containing subject (sub), role, userId, and expiration
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks if the given token has expired.
     *
     * @param token raw JWT token string
     * @return {@code true} if token is expired or unparseable; {@code false} otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            log.warn("Token is expired: {}", e.getMessage());
            return true;
        } catch (Exception e) {
            log.warn("Token parsing error during expiration check: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Validates signature and expiration of the JWT string.
     * Gracefully catches JWT exceptions without propagating errors to the reactive pipeline.
     *
     * @param token raw JWT token string
     * @return {@code true} if token is valid and unexpired; {@code false} otherwise
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            log.warn("Token validation failed: Token is null or blank");
            return false;
        }

        try {
            Claims claims = getClaims(token);
            boolean expired = claims.getExpiration().before(new Date());
            if (expired) {
                log.warn("Token validation failed: Token has expired on {}", claims.getExpiration());
                return false;
            }
            return true;
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty or invalid: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT validation error: {}", e.getMessage());
        }

        return false;
    }
}

package io.provenly.auth.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.provenly.auth.config.JwtProperties;
import io.provenly.auth.model.User;
import io.provenly.commons.exception.ProvenlyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for JWT token generation and validation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI2", justification = "Injected dependency is managed by Spring and not exposed.")
public class JwtService {

    private final JwtProperties jwtProperties;

    /**
     * Generate access token for a user.
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getExpirationHours(), ChronoUnit.HOURS);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("name", user.getName());
        claims.put("roles", user.getRoles());
        
        if (user.getWalletAddress() != null) {
            claims.put("walletAddress", user.getWalletAddress());
        }
        
        if (user.getDid() != null) {
            claims.put("did", user.getDid());
        }

        return Jwts.builder()
                .claims(claims)
                .subject(user.getId().toString())
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generate refresh token.
     */
    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getRefreshExpirationDays(), ChronoUnit.DAYS);

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validate JWT token and return claims.
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            throw new ProvenlyException.AuthenticationException("Token has expired");
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw new ProvenlyException.AuthenticationException("Unsupported token format");
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            throw new ProvenlyException.AuthenticationException("Invalid token format");
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            throw new ProvenlyException.AuthenticationException("Invalid token signature");
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            throw new ProvenlyException.AuthenticationException("Token is empty");
        }
    }

    /**
     * Get user ID from token.
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        String userId = claims.getSubject();
        return UUID.fromString(userId);
    }

    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Get signing key from secret.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Get token expiration time in seconds.
     */
    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.getExpirationHours() * 3600L;
    }

    /**
     * Get refresh token expiration time in seconds.
     */
    public long getRefreshTokenExpirationSeconds() {
        return jwtProperties.getRefreshExpirationDays() * 24L * 3600L;
    }
}


package io.provenly.authlib.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.provenly.authlib.model.TokenPair;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Shared JWT token provider for services using HMAC signing.
 */
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long accessTokenMinutes;
    private final long refreshTokenDays;

    public JwtTokenProvider(String rawSecret, long accessTokenMinutes, long refreshTokenDays) {
        this.signingKey = Keys.hmacShaKeyFor(rawSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
    }

    public TokenPair issueTokenPair(String subject, Map<String, Object> accessClaims) {
        String accessToken = issueAccessToken(subject, accessClaims);
        String refreshToken = issueRefreshToken(subject, UUID.randomUUID().toString());
        return new TokenPair(accessToken, refreshToken);
    }

    public String issueAccessToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public String issueRefreshToken(String subject, String tokenId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(refreshTokenDays, ChronoUnit.DAYS);

        return Jwts.builder()
                .subject(subject)
                .id(tokenId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isExpired(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration != null && expiration.before(new Date());
    }

    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public String getTokenId(String token) {
        return parseClaims(token).getId();
    }
}

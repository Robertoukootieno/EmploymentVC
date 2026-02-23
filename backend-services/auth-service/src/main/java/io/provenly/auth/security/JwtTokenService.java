package io.provenly.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.*;

@Service
public class JwtTokenService {
    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.access-token-expiration-minutes:15}")
    private int accessTokenExpirationMinutes;

    @Value("${security.jwt.refresh-token-expiration-days:7}")
    private int refreshTokenExpirationDays;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateAccessToken(String username, Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMinutes * 60 * 1000))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String username, String tokenId) {
        return Jwts.builder()
                .subject(username)
                .id(tokenId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationDays * 24 * 60 * 60 * 1000))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parseToken(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) key).build().parseSignedClaims(token);
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = parseToken(token).getPayload().getExpiration();
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    public String getUsername(String token) {
        return parseToken(token).getPayload().getSubject();
    }

    public String getTokenId(String token) {
        return parseToken(token).getPayload().getId();
    }
}

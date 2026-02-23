package io.provenly.sdjwtslib.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.provenly.sdjwtslib.model.SdJwtPayload;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * SD-JWT service with selective claim disclosure at presentation time.
 */
public class SdJwtService {

    private static final String DISCLOSABLE_CLAIMS_KEY = "_sd_claims";

    private final SecretKey signingKey;

    public SdJwtService(String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(String subject, SdJwtPayload payload, long validityMinutes) {
        Instant now = Instant.now();
        Instant expiry = now.plus(validityMinutes, ChronoUnit.MINUTES);

        Map<String, Object> claims = new LinkedHashMap<>(payload.getMandatoryClaims());
        claims.put(DISCLOSABLE_CLAIMS_KEY, payload.getDisclosableClaims());

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Map<String, Object> present(String token, Set<String> disclosedClaimKeys) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Map<String, Object> presented = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            if (!DISCLOSABLE_CLAIMS_KEY.equals(entry.getKey())) {
                presented.put(entry.getKey(), entry.getValue());
            }
        }

        Object disclosableObject = claims.get(DISCLOSABLE_CLAIMS_KEY);
        if (disclosableObject instanceof Map<?, ?> disclosableMap) {
            for (String key : disclosedClaimKeys) {
                if (disclosableMap.containsKey(key)) {
                    presented.put(key, disclosableMap.get(key));
                }
            }
        }

        return presented;
    }
}

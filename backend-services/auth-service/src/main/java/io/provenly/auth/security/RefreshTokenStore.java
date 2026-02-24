package io.provenly.auth.security;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory refresh token store for JWT rotation.
 * For production, use Redis or a database.
 */
@Component
public class RefreshTokenStore {
    // tokenId -> username
    private final Map<String, String> validRefreshTokens = new ConcurrentHashMap<>();

    public void store(String tokenId, String username) {
        validRefreshTokens.put(tokenId, username);
    }

    public boolean isValid(String tokenId, String username) {
        return username.equals(validRefreshTokens.get(tokenId));
    }

    public void revoke(String tokenId) {
        validRefreshTokens.remove(tokenId);
    }

    public void revokeAll(String username) {
        validRefreshTokens.entrySet().removeIf(e -> e.getValue().equals(username));
    }
}

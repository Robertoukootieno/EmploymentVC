package io.provenly.auth.repository;

import io.provenly.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshToken entity.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find refresh token by token value.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find all refresh tokens for a user.
     */
    List<RefreshToken> findByUserId(UUID userId);

    /**
     * Find all valid (not expired, not revoked) tokens for a user.
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByUserId(UUID userId, Instant now);

    /**
     * Revoke all tokens for a user.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.userId = :userId")
    void revokeAllByUserId(UUID userId);

    /**
     * Delete expired tokens.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(Instant now);

    /**
     * Delete revoked tokens.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true")
    void deleteRevokedTokens();
}


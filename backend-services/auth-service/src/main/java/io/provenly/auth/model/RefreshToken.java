package io.provenly.auth.model;

import io.provenly.commons.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token entity for token refresh mechanism.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_user_id", columnList = "userId")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseEntity {

    /**
     * The refresh token value.
     */
    @Column(nullable = false, unique = true, length = 500)
    private String token;

    /**
     * User ID this token belongs to.
     */
    @Column(nullable = false)
    private UUID userId;

    /**
     * When the token expires.
     */
    @Column(nullable = false)
    private Instant expiresAt;

    /**
     * Whether the token has been revoked.
     */
    @Builder.Default
    private boolean revoked = false;

    /**
     * IP address from which the token was created.
     */
    private String ipAddress;

    /**
     * User agent from which the token was created.
     */
    private String userAgent;

    /**
     * Check if the token is expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if the token is valid (not expired and not revoked).
     */
    public boolean isValid() {
        return !isExpired() && !revoked;
    }

    /**
     * Revoke the token.
     */
    public void revoke() {
        this.revoked = true;
    }
}


package io.provenly.wallet.model;

import io.provenly.commons.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Stored Credential entity for custodial wallets.
 * Stores the full verifiable credential.
 */
@Entity
@Table(name = "stored_credentials", indexes = {
    @Index(name = "idx_stored_cred_wallet_id", columnList = "walletId"),
    @Index(name = "idx_stored_cred_type", columnList = "credentialType"),
    @Index(name = "idx_stored_cred_status", columnList = "status"),
    @Index(name = "idx_stored_cred_issuer", columnList = "issuerDid")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredCredential extends BaseEntity {

    /**
     * Custodial wallet ID.
     */
    @Column(nullable = false)
    private UUID walletId;

    /**
     * Credential ID (from the VC).
     */
    @Column(nullable = false, unique = true)
    private String credentialId;

    /**
     * Credential type (e.g., EmploymentCredential).
     */
    @Column(nullable = false)
    private String credentialType;

    /**
     * Issuer DID.
     */
    @Column(nullable = false)
    private String issuerDid;

    /**
     * Issuer name.
     */
    private String issuerName;

    /**
     * Subject DID (holder).
     */
    @Column(nullable = false)
    private String subjectDid;

    /**
     * Full verifiable credential (JSON-LD).
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String credentialData;

    /**
     * Credential status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CredentialStatus status = CredentialStatus.ACTIVE;

    /**
     * Issuance date.
     */
    @Column(nullable = false)
    private Instant issuedAt;

    /**
     * Expiration date (if applicable).
     */
    private Instant expiresAt;

    /**
     * Revocation status.
     */
    @Builder.Default
    private boolean revoked = false;

    /**
     * Revocation timestamp.
     */
    private Instant revokedAt;

    /**
     * Revocation reason.
     */
    private String revocationReason;

    /**
     * Tags for organization.
     */
    @Column(columnDefinition = "TEXT")
    private String tags;

    /**
     * Custom metadata (JSON).
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * Last verified timestamp.
     */
    private Instant lastVerifiedAt;

    /**
     * Verification status.
     */
    @Builder.Default
    private boolean verified = false;

    /**
     * Check if credential is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if credential is valid.
     */
    public boolean isValid() {
        return status == CredentialStatus.ACTIVE 
            && !revoked 
            && !isExpired() 
            && verified;
    }

    /**
     * Revoke credential.
     */
    public void revoke(String reason) {
        this.revoked = true;
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
        this.status = CredentialStatus.REVOKED;
    }

    /**
     * Mark as verified.
     */
    public void markAsVerified() {
        this.verified = true;
        this.lastVerifiedAt = Instant.now();
    }

    /**
     * Archive credential.
     */
    public void archive() {
        this.status = CredentialStatus.ARCHIVED;
    }
}


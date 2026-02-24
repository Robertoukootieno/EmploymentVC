package io.provenly.wallet.model;

import io.provenly.commons.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Credential Metadata entity for non-custodial wallets.
 * Stores only metadata, not the actual credential.
 */
@Entity
@Table(name = "credential_metadata", indexes = {
    @Index(name = "idx_cred_meta_wallet_id", columnList = "walletId"),
    @Index(name = "idx_cred_meta_type", columnList = "credentialType"),
    @Index(name = "idx_cred_meta_issuer", columnList = "issuerDid")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredentialMetadata extends BaseEntity {

    /**
     * Non-custodial wallet ID.
     */
    @Column(nullable = false)
    private UUID walletId;

    /**
     * Credential ID (from the VC).
     */
    @Column(nullable = false)
    private String credentialId;

    /**
     * Credential type.
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
     * Subject DID.
     */
    @Column(nullable = false)
    private String subjectDid;

    /**
     * Credential hash (for verification).
     */
    @Column(nullable = false)
    private String credentialHash;

    /**
     * Issuance date.
     */
    @Column(nullable = false)
    private Instant issuedAt;

    /**
     * Expiration date.
     */
    private Instant expiresAt;

    /**
     * Storage location (e.g., IPFS hash, local storage).
     */
    private String storageLocation;

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
     * Last sync timestamp.
     */
    private Instant lastSyncAt;

    /**
     * Check if credential is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Update sync timestamp.
     */
    public void updateSync() {
        this.lastSyncAt = Instant.now();
    }
}


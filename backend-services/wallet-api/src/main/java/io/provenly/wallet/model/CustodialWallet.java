package io.provenly.wallet.model;

import io.provenly.commons.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Custodial Wallet entity.
 * Platform manages the keys and credentials for the user.
 */
@Entity
@Table(name = "custodial_wallets", indexes = {
    @Index(name = "idx_custodial_user_id", columnList = "userId"),
    @Index(name = "idx_custodial_did", columnList = "did"),
    @Index(name = "idx_custodial_status", columnList = "status")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustodialWallet extends BaseEntity {

    /**
     * User ID who owns this wallet.
     */
    @Column(nullable = false)
    private UUID userId;

    /**
     * Wallet name/label.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Wallet description.
     */
    private String description;

    /**
     * Decentralized Identifier (DID) for this wallet.
     */
    @Column(unique = true, nullable = false)
    private String did;

    /**
     * DID method (e.g., did:ebsi, did:key, did:web).
     */
    @Column(nullable = false)
    private String didMethod;

    /**
     * Encrypted private key (for signing).
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedPrivateKey;

    /**
     * Public key (for verification).
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    /**
     * Key type (e.g., Ed25519, SECP256K1).
     */
    @Column(nullable = false)
    private String keyType;

    /**
     * Wallet status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;

    /**
     * Whether this is the default wallet for the user.
     */
    @Builder.Default
    private boolean isDefault = false;

    /**
     * Total number of credentials stored.
     */
    @Builder.Default
    private int credentialCount = 0;

    /**
     * Last activity timestamp.
     */
    private Instant lastActivityAt;

    /**
     * Wallet metadata (JSON).
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * Backup status.
     */
    @Builder.Default
    private boolean backedUp = false;

    /**
     * Last backup timestamp.
     */
    private Instant lastBackupAt;

    /**
     * Update last activity timestamp.
     */
    public void updateActivity() {
        this.lastActivityAt = Instant.now();
    }

    /**
     * Increment credential count.
     */
    public void incrementCredentialCount() {
        this.credentialCount++;
        updateActivity();
    }

    /**
     * Decrement credential count.
     */
    public void decrementCredentialCount() {
        if (this.credentialCount > 0) {
            this.credentialCount--;
        }
        updateActivity();
    }

    /**
     * Mark as backed up.
     */
    public void markAsBackedUp() {
        this.backedUp = true;
        this.lastBackupAt = Instant.now();
    }

    /**
     * Deactivate wallet.
     */
    public void deactivate() {
        this.status = WalletStatus.INACTIVE;
        updateActivity();
    }

    /**
     * Activate wallet.
     */
    public void activate() {
        this.status = WalletStatus.ACTIVE;
        updateActivity();
    }
}


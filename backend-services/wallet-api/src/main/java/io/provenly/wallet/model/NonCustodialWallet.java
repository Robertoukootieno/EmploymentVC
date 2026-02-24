package io.provenly.wallet.model;

import io.provenly.commons.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Non-Custodial Wallet entity.
 * User manages their own keys, platform only stores metadata.
 */
@Entity
@Table(name = "non_custodial_wallets", indexes = {
    @Index(name = "idx_non_custodial_user_id", columnList = "userId"),
    @Index(name = "idx_non_custodial_did", columnList = "did"),
    @Index(name = "idx_non_custodial_wallet_address", columnList = "walletAddress")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NonCustodialWallet extends BaseEntity {

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
     * Decentralized Identifier (DID).
     */
    @Column(unique = true, nullable = false)
    private String did;

    /**
     * DID method.
     */
    @Column(nullable = false)
    private String didMethod;

    /**
     * Public key (for verification only).
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    /**
     * Key type.
     */
    @Column(nullable = false)
    private String keyType;

    /**
     * Ethereum wallet address (if applicable).
     */
    @Column(unique = true)
    private String walletAddress;

    /**
     * Wallet type (e.g., MetaMask, WalletConnect, Hardware).
     */
    private String walletType;

    /**
     * Wallet status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;

    /**
     * Whether this is the default wallet.
     */
    @Builder.Default
    private boolean isDefault = false;

    /**
     * Number of credential metadata entries.
     */
    @Builder.Default
    private int credentialMetadataCount = 0;

    /**
     * Last activity timestamp.
     */
    private Instant lastActivityAt;

    /**
     * Last sync timestamp.
     */
    private Instant lastSyncAt;

    /**
     * Wallet metadata (JSON).
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * Ownership verified flag.
     */
    @Builder.Default
    private boolean ownershipVerified = false;

    /**
     * Ownership verification timestamp.
     */
    private Instant verifiedAt;

    /**
     * Update last activity timestamp.
     */
    public void updateActivity() {
        this.lastActivityAt = Instant.now();
    }

    /**
     * Update last sync timestamp.
     */
    public void updateSync() {
        this.lastSyncAt = Instant.now();
        updateActivity();
    }

    /**
     * Increment credential metadata count.
     */
    public void incrementCredentialMetadataCount() {
        this.credentialMetadataCount++;
        updateActivity();
    }

    /**
     * Decrement credential metadata count.
     */
    public void decrementCredentialMetadataCount() {
        if (this.credentialMetadataCount > 0) {
            this.credentialMetadataCount--;
        }
        updateActivity();
    }

    /**
     * Mark ownership as verified.
     */
    public void verifyOwnership() {
        this.ownershipVerified = true;
        this.verifiedAt = Instant.now();
        updateActivity();
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


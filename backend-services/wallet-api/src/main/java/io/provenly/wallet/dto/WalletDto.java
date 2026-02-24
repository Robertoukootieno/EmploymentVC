package io.provenly.wallet.dto;

import io.provenly.wallet.model.WalletStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Wallet DTO for API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDto {

    /**
     * Wallet ID.
     */
    private UUID id;

    /**
     * Wallet type (CUSTODIAL or NON_CUSTODIAL).
     */
    private String type;

    /**
     * Wallet name.
     */
    private String name;

    /**
     * Wallet description.
     */
    private String description;

    /**
     * DID.
     */
    private String did;

    /**
     * DID method.
     */
    private String didMethod;

    /**
     * Public key.
     */
    private String publicKey;

    /**
     * Key type.
     */
    private String keyType;

    /**
     * Wallet address (for non-custodial Web3 wallets).
     */
    private String walletAddress;

    /**
     * Wallet type (for non-custodial wallets).
     */
    private String walletType;

    /**
     * Wallet status.
     */
    private WalletStatus status;

    /**
     * Whether this is the default wallet.
     */
    private boolean isDefault;

    /**
     * Number of credentials (custodial) or metadata entries (non-custodial).
     */
    private int credentialCount;

    /**
     * Last activity timestamp.
     */
    private Instant lastActivityAt;

    /**
     * Backed up flag (custodial only).
     */
    private Boolean backedUp;

    /**
     * Last backup timestamp (custodial only).
     */
    private Instant lastBackupAt;

    /**
     * Ownership verified flag (non-custodial only).
     */
    private Boolean ownershipVerified;

    /**
     * Verification timestamp (non-custodial only).
     */
    private Instant verifiedAt;

    /**
     * Last sync timestamp (non-custodial only).
     */
    private Instant lastSyncAt;

    /**
     * Custom metadata.
     */
    private String metadata;

    /**
     * Created timestamp.
     */
    private Instant createdAt;

    /**
     * Updated timestamp.
     */
    private Instant updatedAt;
}


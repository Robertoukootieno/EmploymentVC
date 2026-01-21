package io.provenly.wallet.model;

/**
 * Wallet status enumeration.
 */
public enum WalletStatus {
    /**
     * Wallet is active and can be used.
     */
    ACTIVE,

    /**
     * Wallet is temporarily inactive.
     */
    INACTIVE,

    /**
     * Wallet is locked (e.g., due to security concerns).
     */
    LOCKED,

    /**
     * Wallet is archived (soft deleted).
     */
    ARCHIVED
}


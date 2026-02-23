package io.provenly.walletcore.model;

import java.time.Instant;

/**
 * Wallet identity profile.
 */
public class WalletProfile {

    private String walletId;
    private String ownerId;
    private String did;
    private Instant createdAt;

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getDid() {
        return did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

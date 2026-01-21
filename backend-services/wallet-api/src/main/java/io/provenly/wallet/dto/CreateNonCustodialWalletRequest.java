package io.provenly.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for registering a non-custodial wallet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNonCustodialWalletRequest {

    /**
     * Wallet name/label.
     */
    @NotBlank(message = "Wallet name is required")
    private String name;

    /**
     * Wallet description.
     */
    private String description;

    /**
     * DID (user-provided).
     */
    @NotBlank(message = "DID is required")
    @Pattern(regexp = "^did:[a-z0-9]+:[a-zA-Z0-9._%-]*[a-zA-Z0-9]$", message = "Invalid DID format")
    private String did;

    /**
     * DID method.
     */
    @NotBlank(message = "DID method is required")
    private String didMethod;

    /**
     * Public key (for verification).
     */
    @NotBlank(message = "Public key is required")
    private String publicKey;

    /**
     * Key type.
     */
    @NotBlank(message = "Key type is required")
    private String keyType;

    /**
     * Ethereum wallet address (optional, for Web3 wallets).
     */
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "Invalid Ethereum address")
    private String walletAddress;

    /**
     * Wallet type (e.g., "MetaMask", "WalletConnect", "Hardware").
     */
    private String walletType;

    /**
     * Whether to set as default wallet.
     */
    @Builder.Default
    private boolean setAsDefault = false;

    /**
     * Ownership proof (signature).
     */
    @NotBlank(message = "Ownership proof is required")
    private String ownershipProof;

    /**
     * Challenge that was signed for ownership proof.
     */
    @NotBlank(message = "Challenge is required")
    private String challenge;

    /**
     * Custom metadata (JSON string).
     */
    private String metadata;
}


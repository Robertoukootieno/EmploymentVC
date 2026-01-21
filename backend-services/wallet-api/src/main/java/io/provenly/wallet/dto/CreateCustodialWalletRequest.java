package io.provenly.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a custodial wallet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustodialWalletRequest {

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
     * DID method to use (e.g., "ebsi", "key", "web").
     */
    @NotBlank(message = "DID method is required")
    @Pattern(regexp = "^(ebsi|key|web|ion)$", message = "Invalid DID method")
    private String didMethod;

    /**
     * Key type to use (e.g., "Ed25519", "SECP256K1").
     */
    @NotBlank(message = "Key type is required")
    @Pattern(regexp = "^(Ed25519|SECP256K1|SECP256R1|RSA)$", message = "Invalid key type")
    private String keyType;

    /**
     * Whether to set as default wallet.
     */
    @Builder.Default
    private boolean setAsDefault = false;

    /**
     * Custom metadata (JSON string).
     */
    private String metadata;
}


package io.provenly.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for storing a credential in a custodial wallet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreCredentialRequest {

    /**
     * Verifiable credential (JSON-LD).
     */
    @NotBlank(message = "Credential data is required")
    private String credentialData;

    /**
     * Tags for organization (comma-separated).
     */
    private String tags;

    /**
     * Custom metadata (JSON string).
     */
    private String metadata;

    /**
     * Whether to verify the credential immediately.
     */
    @Builder.Default
    private boolean verifyImmediately = true;
}


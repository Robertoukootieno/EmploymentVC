package io.provenly.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request DTO for registering credential metadata in a non-custodial wallet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCredentialMetadataRequest {

    /**
     * Credential ID.
     */
    @NotBlank(message = "Credential ID is required")
    private String credentialId;

    /**
     * Credential type.
     */
    @NotBlank(message = "Credential type is required")
    private String credentialType;

    /**
     * Issuer DID.
     */
    @NotBlank(message = "Issuer DID is required")
    private String issuerDid;

    /**
     * Issuer name.
     */
    private String issuerName;

    /**
     * Subject DID.
     */
    @NotBlank(message = "Subject DID is required")
    private String subjectDid;

    /**
     * Credential hash (for verification).
     */
    @NotBlank(message = "Credential hash is required")
    private String credentialHash;

    /**
     * Issuance date.
     */
    @NotBlank(message = "Issuance date is required")
    private String issuedAt;

    /**
     * Expiration date.
     */
    private String expiresAt;

    /**
     * Storage location (e.g., IPFS hash, local storage path).
     */
    private String storageLocation;

    /**
     * Tags for organization (comma-separated).
     */
    private String tags;

    /**
     * Custom metadata (JSON string).
     */
    private String metadata;
}


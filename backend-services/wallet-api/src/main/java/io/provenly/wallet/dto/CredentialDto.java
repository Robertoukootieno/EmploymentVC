package io.provenly.wallet.dto;

import io.provenly.wallet.model.CredentialStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Credential DTO for API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialDto {

    /**
     * Credential storage ID.
     */
    private UUID id;

    /**
     * Credential ID (from the VC).
     */
    private String credentialId;

    /**
     * Credential type.
     */
    private String credentialType;

    /**
     * Issuer DID.
     */
    private String issuerDid;

    /**
     * Issuer name.
     */
    private String issuerName;

    /**
     * Subject DID.
     */
    private String subjectDid;

    /**
     * Credential data (for custodial wallets).
     */
    private String credentialData;

    /**
     * Credential hash (for non-custodial wallets).
     */
    private String credentialHash;

    /**
     * Storage location (for non-custodial wallets).
     */
    private String storageLocation;

    /**
     * Credential status.
     */
    private CredentialStatus status;

    /**
     * Issuance date.
     */
    private Instant issuedAt;

    /**
     * Expiration date.
     */
    private Instant expiresAt;

    /**
     * Whether credential is expired.
     */
    private boolean expired;

    /**
     * Whether credential is revoked.
     */
    private boolean revoked;

    /**
     * Revocation timestamp.
     */
    private Instant revokedAt;

    /**
     * Revocation reason.
     */
    private String revocationReason;

    /**
     * Whether credential is verified.
     */
    private boolean verified;

    /**
     * Last verified timestamp.
     */
    private Instant lastVerifiedAt;

    /**
     * Tags.
     */
    private String tags;

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


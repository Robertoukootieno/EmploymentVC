package io.provenly.wallet.model;

/**
 * Credential status enumeration.
 */
public enum CredentialStatus {
    /**
     * Credential is active and valid.
     */
    ACTIVE,

    /**
     * Credential has been revoked.
     */
    REVOKED,

    /**
     * Credential has expired.
     */
    EXPIRED,

    /**
     * Credential is suspended temporarily.
     */
    SUSPENDED,

    /**
     * Credential is archived.
     */
    ARCHIVED
}


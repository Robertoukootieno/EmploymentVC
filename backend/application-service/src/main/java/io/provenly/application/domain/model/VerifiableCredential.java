package io.provenly.application.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domain model representing a Verifiable Credential.
 * Supports JSON-LD format with selective disclosure capabilities.
 */
@Entity
@Table(name = "verifiable_credentials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerifiableCredential extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The credential identifier (usually a URI).
     */
    @Column(name = "credential_id", unique = true, nullable = false)
    private String credentialId;

    /**
     * JSON-LD context for the credential.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context", columnDefinition = "jsonb")
    @JsonProperty("@context")
    private List<String> context;

    /**
     * Types of the credential (e.g., ["VerifiableCredential", "EmploymentCredential"]).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "type", columnDefinition = "jsonb")
    private List<String> type;

    /**
     * The issuer of the credential (DID or URI).
     */
    @Column(name = "issuer", nullable = false)
    private String issuer;

    /**
     * When the credential was issued.
     */
    @Column(name = "issuance_date", nullable = false)
    private Instant issuanceDate;

    /**
     * When the credential expires (optional).
     */
    @Column(name = "expiration_date")
    private Instant expirationDate;

    /**
     * The subject of the credential (usually a DID).
     */
    @Column(name = "credential_subject_id", nullable = false)
    private String credentialSubjectId;

    /**
     * The credential subject data as JSON.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "credential_subject", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> credentialSubject;

    /**
     * Proof information for the credential.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proof", columnDefinition = "jsonb")
    private Map<String, Object> proof;

    /**
     * Current status of the credential.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CredentialStatus status = CredentialStatus.ACTIVE;

    /**
     * Schema ID used for this credential.
     */
    @Column(name = "schema_id")
    private String schemaId;

    /**
     * Whether this credential supports selective disclosure.
     */
    @Column(name = "selective_disclosure_enabled")
    @Builder.Default
    private Boolean selectiveDisclosureEnabled = false;

    /**
     * Selective disclosure metadata (if enabled).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selective_disclosure_data", columnDefinition = "jsonb")
    private Map<String, Object> selectiveDisclosureData;

    /**
     * Revocation registry information.
     */
    @Column(name = "revocation_registry_id")
    private String revocationRegistryId;

    /**
     * Revocation index in the registry.
     */
    @Column(name = "revocation_index")
    private Long revocationIndex;

    /**
     * Additional metadata for the credential.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * The wallet that holds this credential (for custodial wallets).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    /**
     * Credential status enumeration.
     */
    public enum CredentialStatus {
        ACTIVE,
        REVOKED,
        SUSPENDED,
        EXPIRED
    }

    /**
     * Check if the credential is expired.
     */
    public boolean isExpired() {
        return expirationDate != null && Instant.now().isAfter(expirationDate);
    }

    /**
     * Check if the credential is active and valid.
     */
    public boolean isValid() {
        return status == CredentialStatus.ACTIVE && !isExpired();
    }

    /**
     * Get the credential as a JSON-LD object.
     */
    public Map<String, Object> toJsonLd() {
        return Map.of(
            "@context", context,
            "id", credentialId,
            "type", type,
            "issuer", issuer,
            "issuanceDate", issuanceDate.toString(),
            "expirationDate", expirationDate != null ? expirationDate.toString() : null,
            "credentialSubject", credentialSubject,
            "proof", proof
        );
    }

    /**
     * Create a credential from JSON-LD data.
     */
    public static VerifiableCredential fromJsonLd(Map<String, Object> jsonLd) {
        return VerifiableCredential.builder()
            .credentialId((String) jsonLd.get("id"))
            .context((List<String>) jsonLd.get("@context"))
            .type((List<String>) jsonLd.get("type"))
            .issuer((String) jsonLd.get("issuer"))
            .issuanceDate(Instant.parse((String) jsonLd.get("issuanceDate")))
            .expirationDate(jsonLd.get("expirationDate") != null ? 
                Instant.parse((String) jsonLd.get("expirationDate")) : null)
            .credentialSubject((Map<String, Object>) jsonLd.get("credentialSubject"))
            .proof((Map<String, Object>) jsonLd.get("proof"))
            .build();
    }
}

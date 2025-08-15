package io.provenly.application.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domain model representing a Wallet for storing Verifiable Credentials.
 * Supports both custodial and non-custodial wallet types.
 */
@Entity
@Table(name = "wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Wallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The owner of the wallet (user ID or DID).
     */
    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    /**
     * The DID associated with this wallet.
     */
    @Column(name = "did")
    private String did;

    /**
     * Type of wallet (custodial or non-custodial).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", nullable = false)
    private WalletType walletType;

    /**
     * Name/label for the wallet.
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Description of the wallet.
     */
    @Column(name = "description")
    private String description;

    /**
     * Whether the wallet is active.
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Wallet configuration and settings.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private Map<String, Object> configuration;

    /**
     * Encryption settings for custodial wallets.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "encryption_settings", columnDefinition = "jsonb")
    private Map<String, Object> encryptionSettings;

    /**
     * Backup and recovery information.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "backup_info", columnDefinition = "jsonb")
    private Map<String, Object> backupInfo;

    /**
     * Access control settings.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "access_control", columnDefinition = "jsonb")
    private Map<String, Object> accessControl;

    /**
     * Credentials stored in this wallet.
     */
    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VerifiableCredential> credentials;

    /**
     * Wallet type enumeration.
     */
    public enum WalletType {
        /**
         * Custodial wallet - keys managed by the platform.
         */
        CUSTODIAL,
        
        /**
         * Non-custodial wallet - user controls their own keys.
         */
        NON_CUSTODIAL,
        
        /**
         * Hardware wallet integration.
         */
        HARDWARE,
        
        /**
         * Multi-signature wallet.
         */
        MULTISIG
    }

    /**
     * Check if this is a custodial wallet.
     */
    public boolean isCustodial() {
        return walletType == WalletType.CUSTODIAL;
    }

    /**
     * Check if this is a non-custodial wallet.
     */
    public boolean isNonCustodial() {
        return walletType == WalletType.NON_CUSTODIAL;
    }

    /**
     * Get the number of credentials in this wallet.
     */
    public int getCredentialCount() {
        return credentials != null ? credentials.size() : 0;
    }

    /**
     * Get active credentials in this wallet.
     */
    public List<VerifiableCredential> getActiveCredentials() {
        return credentials != null ? 
            credentials.stream()
                .filter(VerifiableCredential::isValid)
                .toList() : 
            List.of();
    }
}

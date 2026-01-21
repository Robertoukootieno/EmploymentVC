package io.provenly.wallet.repository;

import io.provenly.wallet.model.CredentialStatus;
import io.provenly.wallet.model.StoredCredential;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for StoredCredential entities.
 */
@Repository
public interface StoredCredentialRepository extends JpaRepository<StoredCredential, UUID> {

    /**
     * Find all credentials for a wallet.
     */
    Page<StoredCredential> findByWalletId(UUID walletId, Pageable pageable);

    /**
     * Find all credentials for a wallet (list).
     */
    List<StoredCredential> findByWalletId(UUID walletId);

    /**
     * Find credential by wallet ID and credential ID.
     */
    Optional<StoredCredential> findByWalletIdAndCredentialId(UUID walletId, String credentialId);

    /**
     * Find credentials by type.
     */
    List<StoredCredential> findByWalletIdAndCredentialType(UUID walletId, String credentialType);

    /**
     * Find credentials by status.
     */
    List<StoredCredential> findByWalletIdAndStatus(UUID walletId, CredentialStatus status);

    /**
     * Find credentials by issuer.
     */
    List<StoredCredential> findByWalletIdAndIssuerDid(UUID walletId, String issuerDid);

    /**
     * Check if credential exists.
     */
    boolean existsByCredentialId(String credentialId);

    /**
     * Count credentials in wallet.
     */
    long countByWalletId(UUID walletId);

    /**
     * Count active credentials in wallet.
     */
    long countByWalletIdAndStatus(UUID walletId, CredentialStatus status);

    /**
     * Find expired credentials.
     */
    @Query("SELECT c FROM StoredCredential c WHERE c.walletId = :walletId AND c.expiresAt < :now AND c.status = 'ACTIVE'")
    List<StoredCredential> findExpiredCredentials(@Param("walletId") UUID walletId, @Param("now") Instant now);

    /**
     * Find credentials expiring soon.
     */
    @Query("SELECT c FROM StoredCredential c WHERE c.walletId = :walletId AND c.expiresAt BETWEEN :now AND :threshold AND c.status = 'ACTIVE'")
    List<StoredCredential> findCredentialsExpiringSoon(
        @Param("walletId") UUID walletId,
        @Param("now") Instant now,
        @Param("threshold") Instant threshold
    );

    /**
     * Search credentials by tags.
     */
    @Query("SELECT c FROM StoredCredential c WHERE c.walletId = :walletId AND c.tags LIKE %:tag%")
    List<StoredCredential> findByWalletIdAndTagsContaining(@Param("walletId") UUID walletId, @Param("tag") String tag);
}


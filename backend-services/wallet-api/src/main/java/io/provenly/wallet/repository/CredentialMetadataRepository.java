package io.provenly.wallet.repository;

import io.provenly.wallet.model.CredentialMetadata;
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
 * Repository for CredentialMetadata entities.
 */
@Repository
public interface CredentialMetadataRepository extends JpaRepository<CredentialMetadata, UUID> {

    /**
     * Find all credential metadata for a wallet.
     */
    Page<CredentialMetadata> findByWalletId(UUID walletId, Pageable pageable);

    /**
     * Find all credential metadata for a wallet (list).
     */
    List<CredentialMetadata> findByWalletId(UUID walletId);

    /**
     * Find credential metadata by wallet ID and credential ID.
     */
    Optional<CredentialMetadata> findByWalletIdAndCredentialId(UUID walletId, String credentialId);

    /**
     * Find credential metadata by type.
     */
    List<CredentialMetadata> findByWalletIdAndCredentialType(UUID walletId, String credentialType);

    /**
     * Find credential metadata by issuer.
     */
    List<CredentialMetadata> findByWalletIdAndIssuerDid(UUID walletId, String issuerDid);

    /**
     * Check if credential metadata exists.
     */
    boolean existsByCredentialId(String credentialId);

    /**
     * Count credential metadata in wallet.
     */
    long countByWalletId(UUID walletId);

    /**
     * Find expired credential metadata.
     */
    @Query("SELECT c FROM CredentialMetadata c WHERE c.walletId = :walletId AND c.expiresAt < :now")
    List<CredentialMetadata> findExpiredCredentials(@Param("walletId") UUID walletId, @Param("now") Instant now);

    /**
     * Find credential metadata expiring soon.
     */
    @Query("SELECT c FROM CredentialMetadata c WHERE c.walletId = :walletId AND c.expiresAt BETWEEN :now AND :threshold")
    List<CredentialMetadata> findCredentialsExpiringSoon(
        @Param("walletId") UUID walletId,
        @Param("now") Instant now,
        @Param("threshold") Instant threshold
    );

    /**
     * Search credential metadata by tags.
     */
    @Query("SELECT c FROM CredentialMetadata c WHERE c.walletId = :walletId AND c.tags LIKE %:tag%")
    List<CredentialMetadata> findByWalletIdAndTagsContaining(@Param("walletId") UUID walletId, @Param("tag") String tag);
}


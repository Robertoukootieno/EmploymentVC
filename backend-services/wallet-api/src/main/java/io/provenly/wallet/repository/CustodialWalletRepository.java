package io.provenly.wallet.repository;

import io.provenly.wallet.model.CustodialWallet;
import io.provenly.wallet.model.WalletStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CustodialWallet entities.
 */
@Repository
public interface CustodialWalletRepository extends JpaRepository<CustodialWallet, UUID> {

    /**
     * Find all wallets for a user.
     */
    List<CustodialWallet> findByUserId(UUID userId);

    /**
     * Find all active wallets for a user.
     */
    List<CustodialWallet> findByUserIdAndStatus(UUID userId, WalletStatus status);

    /**
     * Find wallet by DID.
     */
    Optional<CustodialWallet> findByDid(String did);

    /**
     * Find default wallet for a user.
     */
    Optional<CustodialWallet> findByUserIdAndIsDefaultTrue(UUID userId);

    /**
     * Check if user has any wallets.
     */
    boolean existsByUserId(UUID userId);

    /**
     * Check if DID exists.
     */
    boolean existsByDid(String did);

    /**
     * Count wallets for a user.
     */
    long countByUserId(UUID userId);

    /**
     * Count active wallets for a user.
     */
    long countByUserIdAndStatus(UUID userId, WalletStatus status);

    /**
     * Find wallets by user ID and status.
     */
    @Query("SELECT w FROM CustodialWallet w WHERE w.userId = :userId AND w.status IN :statuses ORDER BY w.isDefault DESC, w.createdAt DESC")
    List<CustodialWallet> findByUserIdAndStatusIn(@Param("userId") UUID userId, @Param("statuses") List<WalletStatus> statuses);

    /**
     * Find wallets that need backup.
     */
    @Query("SELECT w FROM CustodialWallet w WHERE w.userId = :userId AND w.backedUp = false AND w.status = 'ACTIVE'")
    List<CustodialWallet> findWalletsNeedingBackup(@Param("userId") UUID userId);
}


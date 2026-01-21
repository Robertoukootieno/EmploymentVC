package io.provenly.wallet.repository;

import io.provenly.wallet.model.NonCustodialWallet;
import io.provenly.wallet.model.WalletStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for NonCustodialWallet entities.
 */
@Repository
public interface NonCustodialWalletRepository extends JpaRepository<NonCustodialWallet, UUID> {

    /**
     * Find all wallets for a user.
     */
    List<NonCustodialWallet> findByUserId(UUID userId);

    /**
     * Find all active wallets for a user.
     */
    List<NonCustodialWallet> findByUserIdAndStatus(UUID userId, WalletStatus status);

    /**
     * Find wallet by DID.
     */
    Optional<NonCustodialWallet> findByDid(String did);

    /**
     * Find wallet by wallet address.
     */
    Optional<NonCustodialWallet> findByWalletAddress(String walletAddress);

    /**
     * Find default wallet for a user.
     */
    Optional<NonCustodialWallet> findByUserIdAndIsDefaultTrue(UUID userId);

    /**
     * Check if user has any wallets.
     */
    boolean existsByUserId(UUID userId);

    /**
     * Check if DID exists.
     */
    boolean existsByDid(String did);

    /**
     * Check if wallet address exists.
     */
    boolean existsByWalletAddress(String walletAddress);

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
    @Query("SELECT w FROM NonCustodialWallet w WHERE w.userId = :userId AND w.status IN :statuses ORDER BY w.isDefault DESC, w.createdAt DESC")
    List<NonCustodialWallet> findByUserIdAndStatusIn(@Param("userId") UUID userId, @Param("statuses") List<WalletStatus> statuses);

    /**
     * Find unverified wallets for a user.
     */
    @Query("SELECT w FROM NonCustodialWallet w WHERE w.userId = :userId AND w.ownershipVerified = false")
    List<NonCustodialWallet> findUnverifiedWallets(@Param("userId") UUID userId);
}


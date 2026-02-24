package io.provenly.auth.repository;

import io.provenly.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by wallet address.
     */
    Optional<User> findByWalletAddress(String walletAddress);

    /**
     * Find user by DID.
     */
    Optional<User> findByDid(String did);

    /**
     * Find user by Keycloak ID.
     */
    Optional<User> findByKeycloakId(String keycloakId);

    /**
     * Check if email exists.
     */
    boolean existsByEmail(String email);

    /**
     * Check if wallet address exists.
     */
    boolean existsByWalletAddress(String walletAddress);

    /**
     * Check if DID exists.
     */
    boolean existsByDid(String did);
}


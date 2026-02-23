package io.provenly.auth.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.provenly.commons.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * User entity representing a platform user.
 * Supports multiple authentication methods: traditional, Web3 wallet, and DID.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_wallet_address", columnList = "walletAddress"),
    @Index(name = "idx_did", columnList = "did")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressFBWarnings(value = {"EI", "EI2"}, justification = "Roles are defensively copied on access/mutation.")
public class User extends BaseEntity {

    /**
     * User's email address (for traditional auth).
     */
    @Column(unique = true)
    private String email;

    /**
     * User's full name.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Hashed password (for traditional auth).
     */
    private String passwordHash;

    /**
     * Ethereum wallet address (for Web3 auth).
     */
    @Column(unique = true)
    private String walletAddress;

    /**
     * Decentralized Identifier (DID).
     */
    @Column(unique = true)
    private String did;

    /**
     * User roles.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Builder.Default
    private Set<String> roles = new HashSet<>();

    /**
     * Whether the user account is enabled.
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * Whether the email is verified.
     */
    @Builder.Default
    private boolean emailVerified = false;

    /**
     * Last login timestamp.
     */
    private Instant lastLoginAt;

    /**
     * Keycloak user ID (if using Keycloak).
     */
    private String keycloakId;

    /**
     * User metadata (JSON).
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * Check if user has a specific role.
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public Set<String> getRoles() {
        return roles == null ? null : Set.copyOf(roles);
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles == null ? null : new HashSet<>(roles);
    }

    /**
     * Add a role to the user.
     */
    public void addRole(String role) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.add(role);
    }

    /**
     * Remove a role from the user.
     */
    public void removeRole(String role) {
        if (roles != null) {
            roles.remove(role);
        }
    }

    /**
     * Check if user uses traditional authentication.
     */
    public boolean hasTraditionalAuth() {
        return email != null && passwordHash != null;
    }

    /**
     * Check if user uses Web3 authentication.
     */
    public boolean hasWeb3Auth() {
        return walletAddress != null;
    }

    /**
     * Check if user uses DID authentication.
     */
    public boolean hasDidAuth() {
        return did != null;
    }

    /**
     * Update last login timestamp.
     */
    public void updateLastLogin() {
        this.lastLoginAt = Instant.now();
    }
}


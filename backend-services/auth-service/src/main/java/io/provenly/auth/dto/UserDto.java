package io.provenly.auth.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * DTO for user information.
 */
@Data
@NoArgsConstructor
@SuppressFBWarnings(value = {"EI", "EI2"}, justification = "DTO uses defensive copies for mutable fields.")
public class UserDto {

    /**
     * User ID.
     */
    private UUID id;

    /**
     * User's email address.
     */
    private String email;

    /**
     * User's full name.
     */
    private String name;

    /**
     * User roles.
     */
    private Set<String> roles;

    /**
     * Ethereum wallet address (if using Web3 auth).
     */
    private String walletAddress;

    /**
     * Decentralized Identifier (DID).
     */
    private String did;

    /**
     * Whether the email is verified.
     */
    private boolean emailVerified;

    /**
     * Whether the account is enabled.
     */
    private boolean enabled;

    @Builder
    public UserDto(UUID id,
                   String email,
                   String name,
                   Set<String> roles,
                   String walletAddress,
                   String did,
                   boolean emailVerified,
                   boolean enabled) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.roles = roles == null ? null : Set.copyOf(roles);
        this.walletAddress = walletAddress;
        this.did = did;
        this.emailVerified = emailVerified;
        this.enabled = enabled;
    }

    public Set<String> getRoles() {
        return roles == null ? null : Set.copyOf(roles);
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles == null ? null : Set.copyOf(roles);
    }

    public static UserDto copyOf(UserDto source) {
        if (source == null) {
            return null;
        }
        return UserDto.builder()
                .id(source.getId())
                .email(source.getEmail())
                .name(source.getName())
                .roles(source.getRoles())
                .walletAddress(source.getWalletAddress())
                .did(source.getDid())
                .emailVerified(source.isEmailVerified())
                .enabled(source.isEnabled())
                .build();
    }
}


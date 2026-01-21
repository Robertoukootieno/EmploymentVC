package io.provenly.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * DTO for user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}


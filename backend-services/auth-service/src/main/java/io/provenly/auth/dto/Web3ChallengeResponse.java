package io.provenly.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Web3 authentication challenge.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Web3ChallengeResponse {

    /**
     * Challenge message to be signed by the wallet.
     */
    private String challenge;

    /**
     * Unique nonce for this challenge.
     */
    private String nonce;

    /**
     * When the challenge expires (ISO-8601 format).
     */
    private String expiresAt;
}


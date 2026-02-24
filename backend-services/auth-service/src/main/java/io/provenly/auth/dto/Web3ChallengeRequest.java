package io.provenly.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for generating Web3 authentication challenge.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Web3ChallengeRequest {

    /**
     * Ethereum wallet address.
     */
    @NotBlank(message = "Wallet address is required")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "Invalid Ethereum address format")
    private String walletAddress;
}


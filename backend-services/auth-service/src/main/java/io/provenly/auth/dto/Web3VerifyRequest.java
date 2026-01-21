package io.provenly.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for verifying Web3 wallet signature.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Web3VerifyRequest {

    /**
     * Ethereum wallet address.
     */
    @NotBlank(message = "Wallet address is required")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "Invalid Ethereum address format")
    private String walletAddress;

    /**
     * Signature of the challenge message.
     */
    @NotBlank(message = "Signature is required")
    private String signature;

    /**
     * The challenge message that was signed.
     */
    @NotBlank(message = "Message is required")
    private String message;

    /**
     * Chain ID (optional, for EIP-155).
     */
    private Integer chainId;
}


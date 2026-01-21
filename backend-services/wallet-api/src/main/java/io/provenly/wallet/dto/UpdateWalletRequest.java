package io.provenly.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating wallet information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWalletRequest {

    /**
     * Wallet name.
     */
    private String name;

    /**
     * Wallet description.
     */
    private String description;

    /**
     * Whether to set as default wallet.
     */
    private Boolean setAsDefault;

    /**
     * Custom metadata (JSON string).
     */
    private String metadata;
}


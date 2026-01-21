package io.provenly.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for wallet list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletListResponse {

    /**
     * List of wallets.
     */
    private List<WalletDto> wallets;

    /**
     * Total count.
     */
    private long totalCount;

    /**
     * Custodial wallet count.
     */
    private long custodialCount;

    /**
     * Non-custodial wallet count.
     */
    private long nonCustodialCount;
}


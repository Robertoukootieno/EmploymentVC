package io.provenly.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Verifiable presentation DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresentationDto {

    /**
     * Presentation ID.
     */
    private String id;

    /**
     * Presentation type.
     */
    private String type;

    /**
     * Holder DID.
     */
    private String holder;

    /**
     * Verifiable presentation (JSON-LD).
     */
    private String presentationData;

    /**
     * Proof.
     */
    private String proof;

    /**
     * Created timestamp.
     */
    private Instant createdAt;
}


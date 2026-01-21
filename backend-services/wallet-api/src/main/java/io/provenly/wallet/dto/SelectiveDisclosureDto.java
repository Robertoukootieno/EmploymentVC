package io.provenly.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Selective disclosure configuration DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectiveDisclosureDto {

    /**
     * Credential ID.
     */
    private String credentialId;

    /**
     * Fields to disclose.
     */
    private List<String> disclosedFields;

    /**
     * Fields to hide.
     */
    private List<String> hiddenFields;
}


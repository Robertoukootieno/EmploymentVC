package io.provenly.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a verifiable presentation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePresentationRequest {

    /**
     * Credential IDs to include in the presentation.
     */
    @NotEmpty(message = "At least one credential ID is required")
    private List<String> credentialIds;

    /**
     * Challenge from the verifier.
     */
    @NotBlank(message = "Challenge is required")
    private String challenge;

    /**
     * Domain/verifier identifier.
     */
    private String domain;

    /**
     * Selective disclosure configuration.
     */
    private List<SelectiveDisclosureDto> selectiveDisclosure;

    /**
     * Presentation purpose.
     */
    private String purpose;
}


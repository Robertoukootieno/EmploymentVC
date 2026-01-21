package io.provenly.wallet.controller;

import io.provenly.commons.dto.ApiResponse;
import io.provenly.wallet.dto.CreatePresentationRequest;
import io.provenly.wallet.dto.PresentationDto;
import io.provenly.wallet.service.PresentationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for verifiable presentation operations.
 */
@RestController
@RequestMapping("/api/v1/wallets/{walletId}/presentations")
@Tag(name = "Presentations", description = "Verifiable presentation creation and verification endpoints")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RequiredArgsConstructor
public class PresentationController {

    private final PresentationService presentationService;

    /**
     * Create a verifiable presentation.
     */
    @PostMapping
    @Operation(summary = "Create presentation", description = "Create a verifiable presentation from selected credentials with optional selective disclosure")
    public ResponseEntity<ApiResponse<PresentationDto>> createPresentation(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID walletId,
            @Valid @RequestBody CreatePresentationRequest request) {
        
        log.info("Creating presentation for wallet: {} with {} credentials", walletId, request.getCredentialIds().size());
        
        PresentationDto presentation = presentationService.createPresentation(walletId, userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(presentation, "Presentation created successfully"));
    }

    /**
     * Verify a presentation.
     */
    @PostMapping("/verify")
    @Operation(summary = "Verify presentation", description = "Verify a verifiable presentation")
    public ResponseEntity<ApiResponse<Boolean>> verifyPresentation(
            @RequestBody String presentationData) {
        
        log.info("Verifying presentation");
        
        boolean isValid = presentationService.verifyPresentation(presentationData);
        
        String message = isValid ? "Presentation is valid" : "Presentation is invalid";
        
        return ResponseEntity.ok(ApiResponse.success(isValid, message));
    }
}


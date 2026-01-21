package io.provenly.wallet.controller;

import io.provenly.commons.dto.ApiResponse;
import io.provenly.commons.dto.PageResponse;
import io.provenly.wallet.dto.*;
import io.provenly.wallet.model.CredentialStatus;
import io.provenly.wallet.service.CredentialStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for credential storage operations.
 */
@RestController
@RequestMapping("/api/v1/wallets/{walletId}/credentials")
@Tag(name = "Credentials", description = "Credential storage and management endpoints")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RequiredArgsConstructor
public class CredentialController {

    private final CredentialStorageService credentialStorageService;

    /**
     * Store a credential in a custodial wallet.
     */
    @PostMapping
    @Operation(summary = "Store credential", description = "Store a verifiable credential in a custodial wallet")
    public ResponseEntity<ApiResponse<CredentialDto>> storeCredential(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID walletId,
            @Valid @RequestBody StoreCredentialRequest request) {
        
        log.info("Storing credential in wallet: {} for user: {}", walletId, userId);
        
        CredentialDto credential = credentialStorageService.storeCredential(walletId, userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(credential, "Credential stored successfully"));
    }

    /**
     * Register credential metadata in a non-custodial wallet.
     */
    @PostMapping("/metadata")
    @Operation(summary = "Register credential metadata", description = "Register credential metadata in a non-custodial wallet")
    public ResponseEntity<ApiResponse<CredentialDto>> registerCredentialMetadata(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID walletId,
            @Valid @RequestBody RegisterCredentialMetadataRequest request) {
        
        log.info("Registering credential metadata in wallet: {} for user: {}", walletId, userId);
        
        CredentialDto credential = credentialStorageService.registerCredentialMetadata(walletId, userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(credential, "Credential metadata registered successfully"));
    }

    /**
     * Get credential by ID.
     */
    @GetMapping("/{credentialId}")
    @Operation(summary = "Get credential", description = "Retrieve a credential or credential metadata by ID")
    public ResponseEntity<ApiResponse<CredentialDto>> getCredential(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID walletId,
            @PathVariable String credentialId) {
        
        log.debug("Getting credential: {} from wallet: {} for user: {}", credentialId, walletId, userId);
        
        CredentialDto credential = credentialStorageService.getCredential(walletId, credentialId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(credential, "Credential retrieved successfully"));
    }

    /**
     * List credentials in a wallet.
     */
    @GetMapping
    @Operation(summary = "List credentials", description = "List all credentials in a wallet with pagination")
    public ResponseEntity<ApiResponse<PageResponse<CredentialDto>>> listCredentials(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        log.debug("Listing credentials for wallet: {} for user: {}", walletId, userId);
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        PageResponse<CredentialDto> credentials = credentialStorageService.listCredentials(walletId, userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(credentials, "Credentials retrieved successfully"));
    }

    /**
     * Delete credential.
     */
    @DeleteMapping("/{credentialId}")
    @Operation(summary = "Delete credential", description = "Delete a credential or credential metadata")
    public ResponseEntity<ApiResponse<Void>> deleteCredential(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID walletId,
            @PathVariable String credentialId) {
        
        log.info("Deleting credential: {} from wallet: {} for user: {}", credentialId, walletId, userId);
        
        credentialStorageService.deleteCredential(walletId, credentialId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Credential deleted successfully"));
    }

    /**
     * Update credential status.
     */
    @PatchMapping("/{credentialId}/status")
    @Operation(summary = "Update credential status", description = "Update the status of a credential (ACTIVE, REVOKED, EXPIRED, ARCHIVED)")
    public ResponseEntity<ApiResponse<CredentialDto>> updateCredentialStatus(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID walletId,
            @PathVariable String credentialId,
            @RequestParam CredentialStatus status) {
        
        log.info("Updating credential: {} status to: {} for user: {}", credentialId, status, userId);
        
        CredentialDto credential = credentialStorageService.updateCredentialStatus(walletId, credentialId, userId, status);
        
        return ResponseEntity.ok(ApiResponse.success(credential, "Credential status updated successfully"));
    }
}


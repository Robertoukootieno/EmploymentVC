package io.provenly.wallet.controller;

import io.provenly.commons.dto.ApiResponse;
import io.provenly.wallet.dto.*;
import io.provenly.wallet.service.WalletService;
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
 * Controller for non-custodial wallet operations.
 */
@RestController
@RequestMapping("/api/v1/wallets/non-custodial")
@Tag(name = "Non-Custodial Wallets", description = "Non-custodial wallet management endpoints")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RequiredArgsConstructor
public class NonCustodialWalletController {

    private final WalletService walletService;

    /**
     * Register a non-custodial wallet.
     */
    @PostMapping
    @Operation(summary = "Register non-custodial wallet", description = "Register a non-custodial wallet where the user manages their own private keys")
    public ResponseEntity<ApiResponse<WalletDto>> registerNonCustodialWallet(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateNonCustodialWalletRequest request) {
        
        log.info("Registering non-custodial wallet for user: {}", userId);
        
        WalletDto wallet = walletService.registerNonCustodialWallet(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(wallet, "Non-custodial wallet registered successfully"));
    }

    /**
     * Get non-custodial wallet by ID.
     */
    @GetMapping("/{walletId}")
    @Operation(summary = "Get non-custodial wallet", description = "Retrieve a non-custodial wallet by ID")
    public ResponseEntity<ApiResponse<WalletDto>> getNonCustodialWallet(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID walletId) {
        
        log.debug("Getting non-custodial wallet: {} for user: {}", walletId, userId);
        
        WalletDto wallet = walletService.getWalletById(walletId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(wallet, "Non-custodial wallet retrieved successfully"));
    }

    /**
     * Update non-custodial wallet.
     */
    @PutMapping("/{walletId}")
    @Operation(summary = "Update non-custodial wallet", description = "Update non-custodial wallet name or metadata")
    public ResponseEntity<ApiResponse<WalletDto>> updateNonCustodialWallet(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID walletId,
            @Valid @RequestBody UpdateWalletRequest request) {
        
        log.info("Updating non-custodial wallet: {} for user: {}", walletId, userId);
        
        WalletDto wallet = walletService.updateWallet(walletId, userId, request);
        
        return ResponseEntity.ok(ApiResponse.success(wallet, "Non-custodial wallet updated successfully"));
    }

    /**
     * Delete non-custodial wallet.
     */
    @DeleteMapping("/{walletId}")
    @Operation(summary = "Delete non-custodial wallet", description = "Delete a non-custodial wallet")
    public ResponseEntity<ApiResponse<Void>> deleteNonCustodialWallet(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID walletId) {
        
        log.info("Deleting non-custodial wallet: {} for user: {}", walletId, userId);
        
        walletService.deleteWallet(walletId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Non-custodial wallet deleted successfully"));
    }

    /**
     * Set non-custodial wallet as default.
     */
    @PostMapping("/{walletId}/set-default")
    @Operation(summary = "Set default wallet", description = "Set this non-custodial wallet as the default wallet for the user")
    public ResponseEntity<ApiResponse<WalletDto>> setDefaultWallet(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID walletId) {
        
        log.info("Setting non-custodial wallet: {} as default for user: {}", walletId, userId);
        
        WalletDto wallet = walletService.setDefaultWallet(walletId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(wallet, "Default wallet set successfully"));
    }

    /**
     * Get all non-custodial wallets for user.
     */
    @GetMapping
    @Operation(summary = "List non-custodial wallets", description = "Get all non-custodial wallets for the authenticated user")
    public ResponseEntity<ApiResponse<WalletListResponse>> listNonCustodialWallets(
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.debug("Listing non-custodial wallets for user: {}", userId);
        
        WalletListResponse wallets = walletService.getWallets(userId);
        
        return ResponseEntity.ok(ApiResponse.success(wallets, "Non-custodial wallets retrieved successfully"));
    }
}


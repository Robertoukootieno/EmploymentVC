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
 * Controller for custodial wallet operations.
 */
@RestController
@RequestMapping("/api/v1/wallets/custodial")
@Tag(name = "Custodial Wallets", description = "Custodial wallet management endpoints")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RequiredArgsConstructor
public class CustodialWalletController {

    private final WalletService walletService;

    /**
     * Create a new custodial wallet.
     */
    @PostMapping
    @Operation(summary = "Create custodial wallet", description = "Create a new custodial wallet where the platform manages the private keys")
    public ResponseEntity<ApiResponse<WalletDto>> createCustodialWallet(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateCustodialWalletRequest request) {
        
        log.info("Creating custodial wallet for user: {}", userId);
        
        WalletDto wallet = walletService.createCustodialWallet(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(wallet, "Custodial wallet created successfully"));
    }

    /**
     * Get custodial wallet by ID.
     */
    @GetMapping("/{walletId}")
    @Operation(summary = "Get custodial wallet", description = "Retrieve a custodial wallet by ID")
    public ResponseEntity<ApiResponse<WalletDto>> getCustodialWallet(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID walletId) {
        
        log.debug("Getting custodial wallet: {} for user: {}", walletId, userId);
        
        WalletDto wallet = walletService.getWalletById(walletId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(wallet, "Custodial wallet retrieved successfully"));
    }

    /**
     * Update custodial wallet.
     */
    @PutMapping("/{walletId}")
    @Operation(summary = "Update custodial wallet", description = "Update custodial wallet name or metadata")
    public ResponseEntity<ApiResponse<WalletDto>> updateCustodialWallet(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID walletId,
            @Valid @RequestBody UpdateWalletRequest request) {
        
        log.info("Updating custodial wallet: {} for user: {}", walletId, userId);
        
        WalletDto wallet = walletService.updateWallet(walletId, userId, request);
        
        return ResponseEntity.ok(ApiResponse.success(wallet, "Custodial wallet updated successfully"));
    }

    /**
     * Delete custodial wallet.
     */
    @DeleteMapping("/{walletId}")
    @Operation(summary = "Delete custodial wallet", description = "Delete a custodial wallet (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteCustodialWallet(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID walletId) {
        
        log.info("Deleting custodial wallet: {} for user: {}", walletId, userId);
        
        walletService.deleteWallet(walletId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Custodial wallet deleted successfully"));
    }

    /**
     * Set custodial wallet as default.
     */
    @PostMapping("/{walletId}/set-default")
    @Operation(summary = "Set default wallet", description = "Set this custodial wallet as the default wallet for the user")
    public ResponseEntity<ApiResponse<WalletDto>> setDefaultWallet(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID walletId) {
        
        log.info("Setting custodial wallet: {} as default for user: {}", walletId, userId);
        
        WalletDto wallet = walletService.setDefaultWallet(walletId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(wallet, "Default wallet set successfully"));
    }

    /**
     * Get all custodial wallets for user.
     */
    @GetMapping
    @Operation(summary = "List custodial wallets", description = "Get all custodial wallets for the authenticated user")
    public ResponseEntity<ApiResponse<WalletListResponse>> listCustodialWallets(
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.debug("Listing custodial wallets for user: {}", userId);
        
        WalletListResponse wallets = walletService.getWallets(userId);
        
        return ResponseEntity.ok(ApiResponse.success(wallets, "Custodial wallets retrieved successfully"));
    }

    /**
     * Get default custodial wallet.
     */
    @GetMapping("/default")
    @Operation(summary = "Get default wallet", description = "Get the default custodial wallet for the user")
    public ResponseEntity<ApiResponse<WalletDto>> getDefaultWallet(
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.debug("Getting default custodial wallet for user: {}", userId);
        
        WalletDto wallet = walletService.getDefaultWallet(userId);
        
        return ResponseEntity.ok(ApiResponse.success(wallet, "Default wallet retrieved successfully"));
    }
}


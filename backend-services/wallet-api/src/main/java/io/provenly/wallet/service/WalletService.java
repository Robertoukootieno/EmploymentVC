package io.provenly.wallet.service;

import io.provenly.commons.exception.ProvenlyException;
import io.provenly.crypto.model.KeyType;
import io.provenly.crypto.service.EncryptionService;
import io.provenly.crypto.service.KeyGenerationService;
import io.provenly.crypto.service.SigningService;
import io.provenly.wallet.dto.*;
import io.provenly.wallet.model.*;
import io.provenly.wallet.repository.CustodialWalletRepository;
import io.provenly.wallet.repository.NonCustodialWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for wallet management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final CustodialWalletRepository custodialWalletRepository;
    private final NonCustodialWalletRepository nonCustodialWalletRepository;
    private final KeyGenerationService keyGenerationService;
    private final EncryptionService encryptionService;
    private final SigningService signingService;

    @Value("${wallet.encryption.password:change-this-in-production}")
    private String encryptionPassword;

    /**
     * Create a new custodial wallet.
     */
    @Transactional
    public WalletDto createCustodialWallet(UUID userId, CreateCustodialWalletRequest request) {
        log.info("Creating custodial wallet for user: {}", userId);

        try {
            // Parse key type
            KeyType keyType = KeyType.valueOf(request.getKeyType());

            // Generate key pair
            io.provenly.crypto.model.KeyPair keyPair = keyGenerationService.generateKeyPair(keyType);

            // Encrypt private key
            String encryptedPrivateKey = encryptionService.encryptWithPassword(
                keyPair.getPrivateKeyBytes(),
                encryptionPassword
            );

            // Generate DID
            String did = generateDid(request.getDidMethod(), keyPair.getPublicKeyHex());

            // Handle default wallet logic
            if (request.isSetAsDefault()) {
                unsetDefaultWallets(userId, true);
            }

            // Create wallet entity
            CustodialWallet wallet = CustodialWallet.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .did(did)
                .didMethod(request.getDidMethod())
                .encryptedPrivateKey(encryptedPrivateKey)
                .publicKey(keyPair.getPublicKeyBase64())
                .keyType(request.getKeyType())
                .status(WalletStatus.ACTIVE)
                .isDefault(request.isSetAsDefault())
                .metadata(request.getMetadata())
                .lastActivityAt(Instant.now())
                .build();

            wallet = custodialWalletRepository.save(wallet);
            log.info("Created custodial wallet: {}", wallet.getId());

            return mapCustodialWalletToDto(wallet);

        } catch (Exception e) {
            log.error("Failed to create custodial wallet", e);
            throw new ProvenlyException.InternalServerException("Failed to create custodial wallet: " + e.getMessage());
        }
    }

    /**
     * Register a non-custodial wallet.
     */
    @Transactional
    public WalletDto registerNonCustodialWallet(UUID userId, CreateNonCustodialWalletRequest request) {
        log.info("Registering non-custodial wallet for user: {}", userId);

        try {
            // Verify ownership proof
            boolean ownershipVerified = verifyOwnershipProof(
                request.getPublicKey(),
                request.getChallenge(),
                request.getOwnershipProof(),
                KeyType.valueOf(request.getKeyType())
            );

            if (!ownershipVerified) {
                throw new ProvenlyException.UnauthorizedException("Ownership proof verification failed");
            }

            // Check if DID already exists
            if (nonCustodialWalletRepository.existsByDid(request.getDid())) {
                throw new ProvenlyException.ConflictException("Wallet with this DID already exists");
            }

            // Handle default wallet logic
            if (request.isSetAsDefault()) {
                unsetDefaultWallets(userId, false);
            }

            // Create wallet entity
            NonCustodialWallet wallet = NonCustodialWallet.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .did(request.getDid())
                .didMethod(request.getDidMethod())
                .publicKey(request.getPublicKey())
                .keyType(request.getKeyType())
                .walletAddress(request.getWalletAddress())
                .walletType(request.getWalletType())
                .status(WalletStatus.ACTIVE)
                .isDefault(request.isSetAsDefault())
                .ownershipVerified(true)
                .verifiedAt(Instant.now())
                .metadata(request.getMetadata())
                .lastActivityAt(Instant.now())
                .build();

            wallet = nonCustodialWalletRepository.save(wallet);
            log.info("Registered non-custodial wallet: {}", wallet.getId());

            return mapNonCustodialWalletToDto(wallet);

        } catch (ProvenlyException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to register non-custodial wallet", e);
            throw new ProvenlyException.InternalServerException("Failed to register non-custodial wallet: " + e.getMessage());
        }
    }

    /**
     * Get all wallets for a user.
     */
    @Transactional(readOnly = true)
    public WalletListResponse getWallets(UUID userId) {
        log.debug("Getting wallets for user: {}", userId);

        List<CustodialWallet> custodialWallets = custodialWalletRepository.findByUserId(userId);
        List<NonCustodialWallet> nonCustodialWallets = nonCustodialWalletRepository.findByUserId(userId);

        List<WalletDto> allWallets = new ArrayList<>();
        allWallets.addAll(custodialWallets.stream()
            .map(this::mapCustodialWalletToDto)
            .collect(Collectors.toList()));
        allWallets.addAll(nonCustodialWallets.stream()
            .map(this::mapNonCustodialWalletToDto)
            .collect(Collectors.toList()));

        return WalletListResponse.builder()
            .wallets(allWallets)
            .totalCount(allWallets.size())
            .custodialCount(custodialWallets.size())
            .nonCustodialCount(nonCustodialWallets.size())
            .build();
    }

    /**
     * Get wallet by ID.
     */
    @Transactional(readOnly = true)
    public WalletDto getWalletById(UUID walletId, UUID userId) {
        log.debug("Getting wallet: {} for user: {}", walletId, userId);

        // Try custodial first
        return custodialWalletRepository.findById(walletId)
            .filter(w -> w.getUserId().equals(userId))
            .map(this::mapCustodialWalletToDto)
            .orElseGet(() -> nonCustodialWalletRepository.findById(walletId)
                .filter(w -> w.getUserId().equals(userId))
                .map(this::mapNonCustodialWalletToDto)
                .orElseThrow(() -> new ProvenlyException.NotFoundException("Wallet not found")));
    }

    /**
     * Get default wallet for a user.
     */
    @Transactional(readOnly = true)
    public WalletDto getDefaultWallet(UUID userId) {
        log.debug("Getting default wallet for user: {}", userId);

        // Try custodial first
        return custodialWalletRepository.findByUserIdAndIsDefaultTrue(userId)
            .map(this::mapCustodialWalletToDto)
            .orElseGet(() -> nonCustodialWalletRepository.findByUserIdAndIsDefaultTrue(userId)
                .map(this::mapNonCustodialWalletToDto)
                .orElseThrow(() -> new ProvenlyException.NotFoundException("No default wallet found")));
    }

    /**
     * Update wallet.
     */
    @Transactional
    public WalletDto updateWallet(UUID walletId, UUID userId, UpdateWalletRequest request) {
        log.info("Updating wallet: {} for user: {}", walletId, userId);

        // Try custodial first
        var custodialOpt = custodialWalletRepository.findById(walletId)
            .filter(w -> w.getUserId().equals(userId));

        if (custodialOpt.isPresent()) {
            CustodialWallet wallet = custodialOpt.get();
            updateWalletFields(wallet, request);

            if (request.getSetAsDefault() != null && request.getSetAsDefault()) {
                unsetDefaultWallets(userId, true);
                wallet.setDefault(true);
            }

            wallet = custodialWalletRepository.save(wallet);
            return mapCustodialWalletToDto(wallet);
        }

        // Try non-custodial
        NonCustodialWallet wallet = nonCustodialWalletRepository.findById(walletId)
            .filter(w -> w.getUserId().equals(userId))
            .orElseThrow(() -> new ProvenlyException.NotFoundException("Wallet not found"));

        updateWalletFields(wallet, request);

        if (request.getSetAsDefault() != null && request.getSetAsDefault()) {
            unsetDefaultWallets(userId, false);
            wallet.setDefault(true);
        }

        wallet = nonCustodialWalletRepository.save(wallet);
        return mapNonCustodialWalletToDto(wallet);
    }

    /**
     * Delete wallet.
     */
    @Transactional
    public void deleteWallet(UUID walletId, UUID userId) {
        log.info("Deleting wallet: {} for user: {}", walletId, userId);

        // Try custodial first
        var custodialOpt = custodialWalletRepository.findById(walletId)
            .filter(w -> w.getUserId().equals(userId));

        if (custodialOpt.isPresent()) {
            CustodialWallet wallet = custodialOpt.get();
            wallet.setStatus(WalletStatus.ARCHIVED);
            custodialWalletRepository.save(wallet);
            return;
        }

        // Try non-custodial
        NonCustodialWallet wallet = nonCustodialWalletRepository.findById(walletId)
            .filter(w -> w.getUserId().equals(userId))
            .orElseThrow(() -> new ProvenlyException.NotFoundException("Wallet not found"));

        wallet.setStatus(WalletStatus.ARCHIVED);
        nonCustodialWalletRepository.save(wallet);
    }

    /**
     * Set wallet as default.
     */
    @Transactional
    public WalletDto setDefaultWallet(UUID walletId, UUID userId) {
        log.info("Setting wallet {} as default for user: {}", walletId, userId);

        // Try custodial first
        var custodialOpt = custodialWalletRepository.findById(walletId)
            .filter(w -> w.getUserId().equals(userId));

        if (custodialOpt.isPresent()) {
            unsetDefaultWallets(userId, true);
            CustodialWallet wallet = custodialOpt.get();
            wallet.setDefault(true);
            wallet = custodialWalletRepository.save(wallet);
            return mapCustodialWalletToDto(wallet);
        }

        // Try non-custodial
        unsetDefaultWallets(userId, false);
        NonCustodialWallet wallet = nonCustodialWalletRepository.findById(walletId)
            .filter(w -> w.getUserId().equals(userId))
            .orElseThrow(() -> new ProvenlyException.NotFoundException("Wallet not found"));

        wallet.setDefault(true);
        wallet = nonCustodialWalletRepository.save(wallet);
        return mapNonCustodialWalletToDto(wallet);
    }

    // ==================== Helper Methods ====================

    /**
     * Verify ownership proof using signature verification.
     */
    private boolean verifyOwnershipProof(String publicKey, String challenge, String signature, KeyType keyType) {
        try {
            byte[] publicKeyBytes = java.util.Base64.getDecoder().decode(publicKey);
            byte[] challengeBytes = challenge.getBytes();
            return signingService.verify(challengeBytes, signature, publicKeyBytes, keyType);
        } catch (Exception e) {
            log.error("Ownership proof verification failed", e);
            return false;
        }
    }

    /**
     * Generate DID based on method and public key.
     */
    private String generateDid(String didMethod, String publicKeyHex) {
        return switch (didMethod.toLowerCase()) {
            case "key" -> "did:key:z" + publicKeyHex.substring(0, 44);
            case "ebsi" -> "did:ebsi:z" + publicKeyHex.substring(0, 42);
            case "web" -> "did:web:provenly.io:users:" + UUID.randomUUID();
            case "ion" -> "did:ion:" + publicKeyHex.substring(0, 40);
            default -> throw new IllegalArgumentException("Unsupported DID method: " + didMethod);
        };
    }

    /**
     * Unset default wallets for a user.
     */
    private void unsetDefaultWallets(UUID userId, boolean isCustodial) {
        if (isCustodial) {
            custodialWalletRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(wallet -> {
                    wallet.setDefault(false);
                    custodialWalletRepository.save(wallet);
                });
        } else {
            nonCustodialWalletRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(wallet -> {
                    wallet.setDefault(false);
                    nonCustodialWalletRepository.save(wallet);
                });
        }
    }

    /**
     * Update common wallet fields.
     */
    private void updateWalletFields(Object wallet, UpdateWalletRequest request) {
        if (wallet instanceof CustodialWallet cw) {
            if (request.getName() != null) cw.setName(request.getName());
            if (request.getDescription() != null) cw.setDescription(request.getDescription());
            if (request.getMetadata() != null) cw.setMetadata(request.getMetadata());
        } else if (wallet instanceof NonCustodialWallet ncw) {
            if (request.getName() != null) ncw.setName(request.getName());
            if (request.getDescription() != null) ncw.setDescription(request.getDescription());
            if (request.getMetadata() != null) ncw.setMetadata(request.getMetadata());
        }
    }

    /**
     * Map custodial wallet to DTO.
     */
    private WalletDto mapCustodialWalletToDto(CustodialWallet wallet) {
        return WalletDto.builder()
            .id(wallet.getId())
            .type("CUSTODIAL")
            .name(wallet.getName())
            .description(wallet.getDescription())
            .did(wallet.getDid())
            .didMethod(wallet.getDidMethod())
            .publicKey(wallet.getPublicKey())
            .keyType(wallet.getKeyType())
            .status(wallet.getStatus())
            .isDefault(wallet.isDefault())
            .credentialCount(wallet.getCredentialCount())
            .lastActivityAt(wallet.getLastActivityAt())
            .backedUp(wallet.isBackedUp())
            .lastBackupAt(wallet.getLastBackupAt())
            .metadata(wallet.getMetadata())
            .createdAt(wallet.getCreatedAt())
            .updatedAt(wallet.getUpdatedAt())
            .build();
    }

    /**
     * Map non-custodial wallet to DTO.
     */
    private WalletDto mapNonCustodialWalletToDto(NonCustodialWallet wallet) {
        return WalletDto.builder()
            .id(wallet.getId())
            .type("NON_CUSTODIAL")
            .name(wallet.getName())
            .description(wallet.getDescription())
            .did(wallet.getDid())
            .didMethod(wallet.getDidMethod())
            .publicKey(wallet.getPublicKey())
            .keyType(wallet.getKeyType())
            .walletAddress(wallet.getWalletAddress())
            .walletType(wallet.getWalletType())
            .status(wallet.getStatus())
            .isDefault(wallet.isDefault())
            .credentialCount(wallet.getCredentialMetadataCount())
            .lastActivityAt(wallet.getLastActivityAt())
            .ownershipVerified(wallet.isOwnershipVerified())
            .verifiedAt(wallet.getVerifiedAt())
            .metadata(wallet.getMetadata())
            .createdAt(wallet.getCreatedAt())
            .updatedAt(wallet.getUpdatedAt())
            .build();
    }
}

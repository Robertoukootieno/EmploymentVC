package io.provenly.application.wallet.service;

import io.provenly.application.domain.model.Wallet;
import io.provenly.application.domain.model.VerifiableCredential;
import io.provenly.application.wallet.dto.*;
import io.provenly.application.common.exception.WalletException;
import io.provenly.application.repository.WalletRepository;
import io.provenly.application.repository.VerifiableCredentialRepository;
import io.provenly.application.external.WaltIdService;
import io.provenly.application.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing custodial wallets where the platform controls the keys.
 * Provides secure storage and management of Verifiable Credentials.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustodialWalletService {

    private final WalletRepository walletRepository;
    private final VerifiableCredentialRepository credentialRepository;
    private final WaltIdService waltIdService;
    private final EncryptionService encryptionService;
    private final WalletKeyManagementService keyManagementService;

    /**
     * Create a new custodial wallet.
     */
    @Transactional
    public CreateWalletResponse createWallet(CreateCustodialWalletRequest request) {
        try {
            log.info("Creating custodial wallet for owner: {}", request.getOwnerId());

            // Generate DID and keys for the wallet
            var didInfo = keyManagementService.generateDidAndKeys();

            // Create wallet entity
            Wallet wallet = Wallet.builder()
                .ownerId(request.getOwnerId())
                .did(didInfo.getDid())
                .walletType(Wallet.WalletType.CUSTODIAL)
                .name(request.getName())
                .description(request.getDescription())
                .isActive(true)
                .configuration(Map.of(
                    "autoBackup", request.isAutoBackup(),
                    "encryptionEnabled", true,
                    "multiSigEnabled", request.isMultiSigEnabled()
                ))
                .encryptionSettings(Map.of(
                    "algorithm", "AES-256-GCM",
                    "keyDerivation", "PBKDF2"
                ))
                .accessControl(Map.of(
                    "requiresPin", request.isRequiresPin(),
                    "biometricEnabled", request.isBiometricEnabled(),
                    "sessionTimeout", request.getSessionTimeout()
                ))
                .build();

            wallet = walletRepository.save(wallet);

            // Store encrypted keys
            keyManagementService.storeWalletKeys(wallet.getId(), didInfo.getPrivateKey());

            // Create backup if requested
            if (request.isAutoBackup()) {
                createWalletBackup(wallet.getId());
            }

            log.info("Successfully created custodial wallet: {}", wallet.getId());

            return CreateWalletResponse.builder()
                .walletId(wallet.getId())
                .did(wallet.getDid())
                .walletType(wallet.getWalletType())
                .name(wallet.getName())
                .createdAt(wallet.getCreatedAt())
                .build();

        } catch (Exception e) {
            log.error("Failed to create custodial wallet for owner: {}", request.getOwnerId(), e);
            throw new WalletException("Failed to create custodial wallet", e);
        }
    }

    /**
     * Store a credential in a custodial wallet.
     */
    @Transactional
    public StoreCredentialResponse storeCredential(StoreCredentialRequest request) {
        try {
            log.info("Storing credential in wallet: {}", request.getWalletId());

            // Verify wallet ownership and access
            Wallet wallet = verifyWalletAccess(request.getWalletId(), request.getOwnerId());

            // Encrypt credential data
            Map<String, Object> encryptedCredential = encryptionService.encryptCredential(
                request.getCredential(), 
                wallet.getId()
            );

            // Create credential entity
            VerifiableCredential credential = VerifiableCredential.fromJsonLd(request.getCredential());
            credential.setWallet(wallet);
            credential.setMetadata(Map.of(
                "encrypted", true,
                "storageType", "custodial",
                "tags", request.getTags(),
                "notes", request.getNotes()
            ));

            credential = credentialRepository.save(credential);

            // Create backup if auto-backup is enabled
            if (isAutoBackupEnabled(wallet)) {
                createCredentialBackup(credential.getId());
            }

            log.info("Successfully stored credential: {} in wallet: {}", 
                credential.getCredentialId(), wallet.getId());

            return StoreCredentialResponse.builder()
                .credentialId(credential.getCredentialId())
                .walletId(wallet.getId())
                .encrypted(true)
                .storedAt(credential.getCreatedAt())
                .build();

        } catch (Exception e) {
            log.error("Failed to store credential in wallet: {}", request.getWalletId(), e);
            throw new WalletException("Failed to store credential", e);
        }
    }

    /**
     * Retrieve a credential from a custodial wallet.
     */
    public GetCredentialResponse getCredential(GetCredentialRequest request) {
        try {
            log.info("Retrieving credential: {} from wallet: {}", 
                request.getCredentialId(), request.getWalletId());

            // Verify wallet access
            Wallet wallet = verifyWalletAccess(request.getWalletId(), request.getOwnerId());

            // Find credential
            VerifiableCredential credential = credentialRepository
                .findByCredentialIdAndWallet(request.getCredentialId(), wallet)
                .orElseThrow(() -> new WalletException("Credential not found in wallet"));

            // Decrypt credential data
            Map<String, Object> decryptedCredential = encryptionService.decryptCredential(
                credential.toJsonLd(), 
                wallet.getId()
            );

            return GetCredentialResponse.builder()
                .credential(decryptedCredential)
                .metadata(credential.getMetadata())
                .status(credential.getStatus())
                .storedAt(credential.getCreatedAt())
                .build();

        } catch (Exception e) {
            log.error("Failed to retrieve credential: {} from wallet: {}", 
                request.getCredentialId(), request.getWalletId(), e);
            throw new WalletException("Failed to retrieve credential", e);
        }
    }

    /**
     * List credentials in a custodial wallet.
     */
    public ListCredentialsResponse listCredentials(ListCredentialsRequest request) {
        try {
            log.info("Listing credentials in wallet: {}", request.getWalletId());

            // Verify wallet access
            Wallet wallet = verifyWalletAccess(request.getWalletId(), request.getOwnerId());

            // Get credentials with pagination
            PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());
            Page<VerifiableCredential> credentialsPage;

            if (request.getType() != null) {
                credentialsPage = credentialRepository.findByWalletAndTypeContaining(
                    wallet, request.getType(), pageRequest);
            } else {
                credentialsPage = credentialRepository.findByWallet(wallet, pageRequest);
            }

            List<CredentialSummary> credentials = credentialsPage.getContent().stream()
                .map(this::toCredentialSummary)
                .toList();

            return ListCredentialsResponse.builder()
                .credentials(credentials)
                .totalCount(credentialsPage.getTotalElements())
                .page(request.getPage())
                .size(request.getSize())
                .totalPages(credentialsPage.getTotalPages())
                .build();

        } catch (Exception e) {
            log.error("Failed to list credentials in wallet: {}", request.getWalletId(), e);
            throw new WalletException("Failed to list credentials", e);
        }
    }

    /**
     * Create a Verifiable Presentation from selected credentials.
     */
    public CreatePresentationResponse createPresentation(CreatePresentationRequest request) {
        try {
            log.info("Creating presentation from wallet: {}", request.getWalletId());

            // Verify wallet access
            Wallet wallet = verifyWalletAccess(request.getWalletId(), request.getOwnerId());

            // Get selected credentials
            List<VerifiableCredential> credentials = credentialRepository
                .findByCredentialIdInAndWallet(request.getCredentialIds(), wallet);

            if (credentials.size() != request.getCredentialIds().size()) {
                throw new WalletException("Some credentials not found in wallet");
            }

            // Decrypt credentials
            List<Map<String, Object>> decryptedCredentials = credentials.stream()
                .map(cred -> encryptionService.decryptCredential(cred.toJsonLd(), wallet.getId()))
                .toList();

            // Apply selective disclosure if requested
            if (request.getSelectiveDisclosure() != null) {
                decryptedCredentials = applySelectiveDisclosure(
                    decryptedCredentials, 
                    request.getSelectiveDisclosure()
                );
            }

            // Create presentation using walt.id
            Map<String, Object> presentation = waltIdService.createPresentation(
                decryptedCredentials,
                wallet.getDid(),
                request.getChallenge(),
                request.getDomain()
            );

            log.info("Successfully created presentation from wallet: {}", wallet.getId());

            return CreatePresentationResponse.builder()
                .presentation(presentation)
                .holder(wallet.getDid())
                .credentialCount(credentials.size())
                .createdAt(java.time.Instant.now())
                .build();

        } catch (Exception e) {
            log.error("Failed to create presentation from wallet: {}", request.getWalletId(), e);
            throw new WalletException("Failed to create presentation", e);
        }
    }

    /**
     * Delete a credential from a custodial wallet.
     */
    @Transactional
    public void deleteCredential(DeleteCredentialRequest request) {
        try {
            log.info("Deleting credential: {} from wallet: {}", 
                request.getCredentialId(), request.getWalletId());

            // Verify wallet access
            Wallet wallet = verifyWalletAccess(request.getWalletId(), request.getOwnerId());

            // Find and delete credential
            VerifiableCredential credential = credentialRepository
                .findByCredentialIdAndWallet(request.getCredentialId(), wallet)
                .orElseThrow(() -> new WalletException("Credential not found in wallet"));

            credentialRepository.delete(credential);

            log.info("Successfully deleted credential: {} from wallet: {}", 
                request.getCredentialId(), wallet.getId());

        } catch (Exception e) {
            log.error("Failed to delete credential: {} from wallet: {}", 
                request.getCredentialId(), request.getWalletId(), e);
            throw new WalletException("Failed to delete credential", e);
        }
    }

    /**
     * Get wallet information.
     */
    public WalletInfoResponse getWalletInfo(UUID walletId, String ownerId) {
        try {
            Wallet wallet = verifyWalletAccess(walletId, ownerId);
            
            long credentialCount = credentialRepository.countByWallet(wallet);
            long activeCredentialCount = credentialRepository.countByWalletAndStatus(
                wallet, VerifiableCredential.CredentialStatus.ACTIVE);

            return WalletInfoResponse.builder()
                .walletId(wallet.getId())
                .did(wallet.getDid())
                .name(wallet.getName())
                .description(wallet.getDescription())
                .walletType(wallet.getWalletType())
                .isActive(wallet.getIsActive())
                .credentialCount(credentialCount)
                .activeCredentialCount(activeCredentialCount)
                .configuration(wallet.getConfiguration())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();

        } catch (Exception e) {
            log.error("Failed to get wallet info: {}", walletId, e);
            throw new WalletException("Failed to get wallet info", e);
        }
    }

    // Private helper methods

    private Wallet verifyWalletAccess(UUID walletId, String ownerId) {
        Wallet wallet = walletRepository.findById(walletId)
            .orElseThrow(() -> new WalletException("Wallet not found"));

        if (!wallet.getOwnerId().equals(ownerId)) {
            throw new WalletException("Unauthorized access to wallet");
        }

        if (!wallet.getIsActive()) {
            throw new WalletException("Wallet is not active");
        }

        return wallet;
    }

    private boolean isAutoBackupEnabled(Wallet wallet) {
        return wallet.getConfiguration() != null && 
               Boolean.TRUE.equals(wallet.getConfiguration().get("autoBackup"));
    }

    private void createWalletBackup(UUID walletId) {
        // Implementation for wallet backup
        log.info("Creating backup for wallet: {}", walletId);
    }

    private void createCredentialBackup(UUID credentialId) {
        // Implementation for credential backup
        log.info("Creating backup for credential: {}", credentialId);
    }

    private CredentialSummary toCredentialSummary(VerifiableCredential credential) {
        return CredentialSummary.builder()
            .credentialId(credential.getCredentialId())
            .type(credential.getType())
            .issuer(credential.getIssuer())
            .issuanceDate(credential.getIssuanceDate())
            .expirationDate(credential.getExpirationDate())
            .status(credential.getStatus())
            .build();
    }

    private List<Map<String, Object>> applySelectiveDisclosure(
            List<Map<String, Object>> credentials,
            Map<String, List<String>> selectiveDisclosure) {
        // Implementation for applying selective disclosure
        return credentials; // Simplified for now
    }
}

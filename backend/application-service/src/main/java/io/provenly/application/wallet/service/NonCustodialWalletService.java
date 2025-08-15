package io.provenly.application.wallet.service;

import io.provenly.application.domain.model.Wallet;
import io.provenly.application.domain.model.VerifiableCredential;
import io.provenly.application.wallet.dto.*;
import io.provenly.application.common.exception.WalletException;
import io.provenly.application.repository.WalletRepository;
import io.provenly.application.repository.VerifiableCredentialRepository;
import io.provenly.application.external.WaltIdService;
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
 * Service for managing non-custodial wallets where users control their own keys.
 * Provides credential metadata management while users maintain control of their credentials.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NonCustodialWalletService {

    private final WalletRepository walletRepository;
    private final VerifiableCredentialRepository credentialRepository;
    private final WaltIdService waltIdService;

    /**
     * Register a non-custodial wallet (user provides their own DID).
     */
    @Transactional
    public CreateWalletResponse registerWallet(RegisterNonCustodialWalletRequest request) {
        try {
            log.info("Registering non-custodial wallet for owner: {} with DID: {}", 
                request.getOwnerId(), request.getDid());

            // Verify DID ownership by requiring a signature
            if (!verifyDidOwnership(request.getDid(), request.getOwnershipProof())) {
                throw new WalletException("Failed to verify DID ownership");
            }

            // Check if wallet already exists for this DID
            if (walletRepository.findByDid(request.getDid()).isPresent()) {
                throw new WalletException("Wallet already registered for this DID");
            }

            // Create wallet entity (metadata only)
            Wallet wallet = Wallet.builder()
                .ownerId(request.getOwnerId())
                .did(request.getDid())
                .walletType(Wallet.WalletType.NON_CUSTODIAL)
                .name(request.getName())
                .description(request.getDescription())
                .isActive(true)
                .configuration(Map.of(
                    "userControlled", true,
                    "metadataOnly", true,
                    "syncEnabled", request.isSyncEnabled()
                ))
                .accessControl(Map.of(
                    "requiresSignature", true,
                    "didVerificationRequired", true
                ))
                .build();

            wallet = walletRepository.save(wallet);

            log.info("Successfully registered non-custodial wallet: {}", wallet.getId());

            return CreateWalletResponse.builder()
                .walletId(wallet.getId())
                .did(wallet.getDid())
                .walletType(wallet.getWalletType())
                .name(wallet.getName())
                .createdAt(wallet.getCreatedAt())
                .build();

        } catch (Exception e) {
            log.error("Failed to register non-custodial wallet for owner: {}", 
                request.getOwnerId(), e);
            throw new WalletException("Failed to register non-custodial wallet", e);
        }
    }

    /**
     * Register credential metadata (credential stored externally by user).
     */
    @Transactional
    public RegisterCredentialMetadataResponse registerCredentialMetadata(
            RegisterCredentialMetadataRequest request) {
        try {
            log.info("Registering credential metadata for wallet: {}", request.getWalletId());

            // Verify wallet access and DID ownership
            Wallet wallet = verifyWalletAccess(request.getWalletId(), request.getOwnerId());
            
            if (!verifyCredentialOwnership(request.getCredentialProof(), wallet.getDid())) {
                throw new WalletException("Failed to verify credential ownership");
            }

            // Verify the credential is valid (without storing it)
            if (!waltIdService.verifyCredentialProof(request.getCredentialMetadata())) {
                throw new WalletException("Invalid credential provided");
            }

            // Create credential metadata entry
            VerifiableCredential credentialMetadata = VerifiableCredential.builder()
                .credentialId(request.getCredentialId())
                .context((List<String>) request.getCredentialMetadata().get("@context"))
                .type((List<String>) request.getCredentialMetadata().get("type"))
                .issuer((String) request.getCredentialMetadata().get("issuer"))
                .issuanceDate(java.time.Instant.parse(
                    (String) request.getCredentialMetadata().get("issuanceDate")))
                .expirationDate(request.getCredentialMetadata().get("expirationDate") != null ?
                    java.time.Instant.parse(
                        (String) request.getCredentialMetadata().get("expirationDate")) : null)
                .credentialSubjectId(wallet.getDid())
                .credentialSubject(Map.of()) // Empty - user controls actual data
                .wallet(wallet)
                .metadata(Map.of(
                    "userControlled", true,
                    "metadataOnly", true,
                    "externalStorage", true,
                    "tags", request.getTags(),
                    "notes", request.getNotes(),
                    "storageLocation", request.getStorageLocation()
                ))
                .build();

            credentialMetadata = credentialRepository.save(credentialMetadata);

            log.info("Successfully registered credential metadata: {} for wallet: {}", 
                request.getCredentialId(), wallet.getId());

            return RegisterCredentialMetadataResponse.builder()
                .credentialId(credentialMetadata.getCredentialId())
                .walletId(wallet.getId())
                .registeredAt(credentialMetadata.getCreatedAt())
                .build();

        } catch (Exception e) {
            log.error("Failed to register credential metadata for wallet: {}", 
                request.getWalletId(), e);
            throw new WalletException("Failed to register credential metadata", e);
        }
    }

    /**
     * List credential metadata for a non-custodial wallet.
     */
    public ListCredentialsResponse listCredentialMetadata(ListCredentialsRequest request) {
        try {
            log.info("Listing credential metadata for wallet: {}", request.getWalletId());

            // Verify wallet access
            Wallet wallet = verifyWalletAccess(request.getWalletId(), request.getOwnerId());

            // Get credential metadata with pagination
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
            log.error("Failed to list credential metadata for wallet: {}", request.getWalletId(), e);
            throw new WalletException("Failed to list credential metadata", e);
        }
    }

    /**
     * Verify a presentation created by the user (non-custodial).
     */
    public VerifyPresentationResponse verifyUserPresentation(VerifyUserPresentationRequest request) {
        try {
            log.info("Verifying user-created presentation for wallet: {}", request.getWalletId());

            // Verify wallet access
            Wallet wallet = verifyWalletAccess(request.getWalletId(), request.getOwnerId());

            // Verify the presentation was created by the wallet owner
            String presentationHolder = (String) request.getPresentation().get("holder");
            if (!wallet.getDid().equals(presentationHolder)) {
                throw new WalletException("Presentation holder does not match wallet DID");
            }

            // Verify presentation proof
            boolean isValid = waltIdService.verifyPresentationProof(
                request.getPresentation(),
                request.getChallenge(),
                request.getDomain()
            );

            // Verify each credential in the presentation
            List<Map<String, Object>> credentials = (List<Map<String, Object>>) 
                request.getPresentation().get("verifiableCredential");

            List<CredentialVerificationResult> credentialResults = credentials.stream()
                .map(credential -> verifyCredentialInPresentation(credential, wallet))
                .toList();

            boolean allCredentialsValid = credentialResults.stream()
                .allMatch(CredentialVerificationResult::isValid);

            return VerifyPresentationResponse.builder()
                .valid(isValid && allCredentialsValid)
                .holder(presentationHolder)
                .credentialResults(credentialResults)
                .verifiedAt(java.time.Instant.now())
                .build();

        } catch (Exception e) {
            log.error("Failed to verify user presentation for wallet: {}", request.getWalletId(), e);
            throw new WalletException("Failed to verify user presentation", e);
        }
    }

    /**
     * Update credential metadata.
     */
    @Transactional
    public void updateCredentialMetadata(UpdateCredentialMetadataRequest request) {
        try {
            log.info("Updating credential metadata: {} in wallet: {}", 
                request.getCredentialId(), request.getWalletId());

            // Verify wallet access
            Wallet wallet = verifyWalletAccess(request.getWalletId(), request.getOwnerId());

            // Find credential metadata
            VerifiableCredential credential = credentialRepository
                .findByCredentialIdAndWallet(request.getCredentialId(), wallet)
                .orElseThrow(() -> new WalletException("Credential metadata not found"));

            // Update metadata
            Map<String, Object> updatedMetadata = credential.getMetadata();
            if (request.getTags() != null) {
                updatedMetadata.put("tags", request.getTags());
            }
            if (request.getNotes() != null) {
                updatedMetadata.put("notes", request.getNotes());
            }
            if (request.getStorageLocation() != null) {
                updatedMetadata.put("storageLocation", request.getStorageLocation());
            }

            credential.setMetadata(updatedMetadata);
            credentialRepository.save(credential);

            log.info("Successfully updated credential metadata: {}", request.getCredentialId());

        } catch (Exception e) {
            log.error("Failed to update credential metadata: {}", request.getCredentialId(), e);
            throw new WalletException("Failed to update credential metadata", e);
        }
    }

    /**
     * Remove credential metadata.
     */
    @Transactional
    public void removeCredentialMetadata(RemoveCredentialMetadataRequest request) {
        try {
            log.info("Removing credential metadata: {} from wallet: {}", 
                request.getCredentialId(), request.getWalletId());

            // Verify wallet access
            Wallet wallet = verifyWalletAccess(request.getWalletId(), request.getOwnerId());

            // Find and remove credential metadata
            VerifiableCredential credential = credentialRepository
                .findByCredentialIdAndWallet(request.getCredentialId(), wallet)
                .orElseThrow(() -> new WalletException("Credential metadata not found"));

            credentialRepository.delete(credential);

            log.info("Successfully removed credential metadata: {}", request.getCredentialId());

        } catch (Exception e) {
            log.error("Failed to remove credential metadata: {}", request.getCredentialId(), e);
            throw new WalletException("Failed to remove credential metadata", e);
        }
    }

    /**
     * Get wallet information for non-custodial wallet.
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

        if (wallet.getWalletType() != Wallet.WalletType.NON_CUSTODIAL) {
            throw new WalletException("Not a non-custodial wallet");
        }

        return wallet;
    }

    private boolean verifyDidOwnership(String did, Map<String, Object> ownershipProof) {
        try {
            // Verify that the user controls the DID by checking a signature
            return waltIdService.verifyDidOwnership(did, ownershipProof);
        } catch (Exception e) {
            log.error("Failed to verify DID ownership for: {}", did, e);
            return false;
        }
    }

    private boolean verifyCredentialOwnership(Map<String, Object> credentialProof, String did) {
        try {
            // Verify that the credential belongs to the DID holder
            return waltIdService.verifyCredentialOwnership(credentialProof, did);
        } catch (Exception e) {
            log.error("Failed to verify credential ownership for DID: {}", did, e);
            return false;
        }
    }

    private CredentialVerificationResult verifyCredentialInPresentation(
            Map<String, Object> credential, Wallet wallet) {
        try {
            // Verify the credential is valid
            boolean isValid = waltIdService.verifyCredentialProof(credential);
            
            // Check if we have metadata for this credential
            String credentialId = (String) credential.get("id");
            boolean hasMetadata = credentialRepository
                .findByCredentialIdAndWallet(credentialId, wallet)
                .isPresent();

            return CredentialVerificationResult.builder()
                .credentialId(credentialId)
                .valid(isValid)
                .hasMetadata(hasMetadata)
                .message(isValid ? "Valid credential" : "Invalid credential")
                .build();

        } catch (Exception e) {
            log.error("Failed to verify credential in presentation", e);
            return CredentialVerificationResult.builder()
                .credentialId((String) credential.get("id"))
                .valid(false)
                .hasMetadata(false)
                .message("Verification failed: " + e.getMessage())
                .build();
        }
    }

    private CredentialSummary toCredentialSummary(VerifiableCredential credential) {
        return CredentialSummary.builder()
            .credentialId(credential.getCredentialId())
            .type(credential.getType())
            .issuer(credential.getIssuer())
            .issuanceDate(credential.getIssuanceDate())
            .expirationDate(credential.getExpirationDate())
            .status(credential.getStatus())
            .metadata(credential.getMetadata())
            .build();
    }
}

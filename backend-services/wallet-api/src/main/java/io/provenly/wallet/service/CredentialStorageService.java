package io.provenly.wallet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.provenly.commons.dto.PageResponse;
import io.provenly.commons.exception.ProvenlyException;
import io.provenly.wallet.dto.*;
import io.provenly.wallet.model.*;
import io.provenly.wallet.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for credential storage operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CredentialStorageService {

    private final StoredCredentialRepository storedCredentialRepository;
    private final CredentialMetadataRepository credentialMetadataRepository;
    private final CustodialWalletRepository custodialWalletRepository;
    private final NonCustodialWalletRepository nonCustodialWalletRepository;
    private final ObjectMapper objectMapper;

    /**
     * Store a credential in a custodial wallet.
     */
    @Transactional
    public CredentialDto storeCredential(UUID walletId, UUID userId, StoreCredentialRequest request) {
        log.info("Storing credential in custodial wallet: {}", walletId);

        // Verify wallet exists and belongs to user
        CustodialWallet wallet = custodialWalletRepository.findById(walletId)
            .filter(w -> w.getUserId().equals(userId))
            .orElseThrow(() -> new ProvenlyException.NotFoundException("Custodial wallet not found"));

        try {
            // Parse credential data to extract metadata
            var credentialJson = objectMapper.readTree(request.getCredentialData());
            
            String credentialId = credentialJson.has("id") ? credentialJson.get("id").asText() : UUID.randomUUID().toString();
            String credentialType = extractCredentialType(credentialJson);
            String issuerDid = extractIssuerDid(credentialJson);
            String issuerName = extractIssuerName(credentialJson);
            String subjectDid = extractSubjectDid(credentialJson);
            Instant issuedAt = extractIssuedAt(credentialJson);
            Instant expiresAt = extractExpiresAt(credentialJson);

            // Check if credential already exists
            if (storedCredentialRepository.existsByCredentialId(credentialId)) {
                throw new ProvenlyException.ConflictException("Credential already exists");
            }

            // Create stored credential
            StoredCredential credential = StoredCredential.builder()
                .walletId(walletId)
                .credentialId(credentialId)
                .credentialType(credentialType)
                .issuerDid(issuerDid)
                .issuerName(issuerName)
                .subjectDid(subjectDid)
                .credentialData(request.getCredentialData())
                .status(CredentialStatus.ACTIVE)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .tags(request.getTags())
                .metadata(request.getMetadata())
                .verified(request.isVerifyImmediately())
                .lastVerifiedAt(request.isVerifyImmediately() ? Instant.now() : null)
                .build();

            credential = storedCredentialRepository.save(credential);

            // Update wallet credential count
            wallet.incrementCredentialCount();
            custodialWalletRepository.save(wallet);

            log.info("Stored credential: {} in wallet: {}", credentialId, walletId);
            return mapStoredCredentialToDto(credential);

        } catch (ProvenlyException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to store credential", e);
            throw new ProvenlyException.InternalServerException("Failed to store credential: " + e.getMessage());
        }
    }

    /**
     * Register credential metadata in a non-custodial wallet.
     */
    @Transactional
    public CredentialDto registerCredentialMetadata(UUID walletId, UUID userId, RegisterCredentialMetadataRequest request) {
        log.info("Registering credential metadata in non-custodial wallet: {}", walletId);

        // Verify wallet exists and belongs to user
        NonCustodialWallet wallet = nonCustodialWalletRepository.findById(walletId)
            .filter(w -> w.getUserId().equals(userId))
            .orElseThrow(() -> new ProvenlyException.NotFoundException("Non-custodial wallet not found"));

        try {
            // Check if credential metadata already exists
            if (credentialMetadataRepository.existsByCredentialId(request.getCredentialId())) {
                throw new ProvenlyException.ConflictException("Credential metadata already exists");
            }

            // Parse dates
            Instant issuedAt = Instant.parse(request.getIssuedAt());
            Instant expiresAt = request.getExpiresAt() != null ? Instant.parse(request.getExpiresAt()) : null;

            // Create credential metadata
            CredentialMetadata metadata = CredentialMetadata.builder()
                .walletId(walletId)
                .credentialId(request.getCredentialId())
                .credentialType(request.getCredentialType())
                .issuerDid(request.getIssuerDid())
                .issuerName(request.getIssuerName())
                .subjectDid(request.getSubjectDid())
                .credentialHash(request.getCredentialHash())
                .storageLocation(request.getStorageLocation())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .tags(request.getTags())
                .metadata(request.getMetadata())
                .build();

            metadata = credentialMetadataRepository.save(metadata);

            // Update wallet credential metadata count
            wallet.incrementCredentialMetadataCount();
            nonCustodialWalletRepository.save(wallet);

            log.info("Registered credential metadata: {} in wallet: {}", request.getCredentialId(), walletId);
            return mapCredentialMetadataToDto(metadata);

        } catch (ProvenlyException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to register credential metadata", e);
            throw new ProvenlyException.InternalServerException("Failed to register credential metadata: " + e.getMessage());
        }
    }

    /**
     * Get credential by ID.
     */
    @Transactional(readOnly = true)
    public CredentialDto getCredential(UUID walletId, String credentialId, UUID userId) {
        log.debug("Getting credential: {} from wallet: {}", credentialId, walletId);

        // Try custodial wallet first
        var custodialOpt = custodialWalletRepository.findById(walletId)
            .filter(w -> w.getUserId().equals(userId));

        if (custodialOpt.isPresent()) {
            return storedCredentialRepository.findByWalletIdAndCredentialId(walletId, credentialId)
                .map(this::mapStoredCredentialToDto)
                .orElseThrow(() -> new ProvenlyException.NotFoundException("Credential not found"));
        }

        // Try non-custodial wallet
        nonCustodialWalletRepository.findById(walletId)
            .filter(w -> w.getUserId().equals(userId))
            .orElseThrow(() -> new ProvenlyException.NotFoundException("Wallet not found"));

        return credentialMetadataRepository.findByWalletIdAndCredentialId(walletId, credentialId)
            .map(this::mapCredentialMetadataToDto)
            .orElseThrow(() -> new ProvenlyException.NotFoundException("Credential metadata not found"));
    }

    /**
     * List credentials in a wallet.
     */
    @Transactional(readOnly = true)
    public PageResponse<CredentialDto> listCredentials(UUID walletId, UUID userId, Pageable pageable) {
        log.debug("Listing credentials for wallet: {}", walletId);

        // Try custodial wallet first
        var custodialOpt = custodialWalletRepository.findById(walletId)
            .filter(w -> w.getUserId().equals(userId));

        if (custodialOpt.isPresent()) {
            Page<StoredCredential> page = storedCredentialRepository.findByWalletId(walletId, pageable);
            List<CredentialDto> credentials = page.getContent().stream()
                .map(this::mapStoredCredentialToDto)
                .collect(Collectors.toList());

            return PageResponse.<CredentialDto>builder()
                .content(credentials)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
        }

        // Try non-custodial wallet
        nonCustodialWalletRepository.findById(walletId)
            .filter(w -> w.getUserId().equals(userId))
            .orElseThrow(() -> new ProvenlyException.NotFoundException("Wallet not found"));

        Page<CredentialMetadata> page = credentialMetadataRepository.findByWalletId(walletId, pageable);
        List<CredentialDto> credentials = page.getContent().stream()
            .map(this::mapCredentialMetadataToDto)
            .collect(Collectors.toList());

        return PageResponse.<CredentialDto>builder()
            .content(credentials)
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .build();
    }

    /**
     * Delete credential.
     */
    @Transactional
    public void deleteCredential(UUID walletId, String credentialId, UUID userId) {
        log.info("Deleting credential: {} from wallet: {}", credentialId, walletId);

        // Try custodial wallet first
        var custodialOpt = custodialWalletRepository.findById(walletId)
            .filter(w -> w.getUserId().equals(userId));

        if (custodialOpt.isPresent()) {
            StoredCredential credential = storedCredentialRepository.findByWalletIdAndCredentialId(walletId, credentialId)
                .orElseThrow(() -> new ProvenlyException.NotFoundException("Credential not found"));

            credential.setStatus(CredentialStatus.ARCHIVED);
            storedCredentialRepository.save(credential);

            // Update wallet credential count
            CustodialWallet wallet = custodialOpt.get();
            wallet.setCredentialCount(Math.max(0, wallet.getCredentialCount() - 1));
            custodialWalletRepository.save(wallet);
            return;
        }

        // Try non-custodial wallet
        var nonCustodialOpt = nonCustodialWalletRepository.findById(walletId)
            .filter(w -> w.getUserId().equals(userId));

        if (nonCustodialOpt.isPresent()) {
            CredentialMetadata metadata = credentialMetadataRepository.findByWalletIdAndCredentialId(walletId, credentialId)
                .orElseThrow(() -> new ProvenlyException.NotFoundException("Credential metadata not found"));

            credentialMetadataRepository.delete(metadata);

            // Update wallet credential metadata count
            NonCustodialWallet wallet = nonCustodialOpt.get();
            wallet.setCredentialMetadataCount(Math.max(0, wallet.getCredentialMetadataCount() - 1));
            nonCustodialWalletRepository.save(wallet);
            return;
        }

        throw new ProvenlyException.NotFoundException("Wallet not found");
    }

    /**
     * Update credential status.
     */
    @Transactional
    public CredentialDto updateCredentialStatus(UUID walletId, String credentialId, UUID userId, CredentialStatus newStatus) {
        log.info("Updating credential status: {} to {}", credentialId, newStatus);

        // Only works for custodial wallets
        custodialWalletRepository.findById(walletId)
            .filter(w -> w.getUserId().equals(userId))
            .orElseThrow(() -> new ProvenlyException.NotFoundException("Custodial wallet not found"));

        StoredCredential credential = storedCredentialRepository.findByWalletIdAndCredentialId(walletId, credentialId)
            .orElseThrow(() -> new ProvenlyException.NotFoundException("Credential not found"));

        credential.setStatus(newStatus);
        if (newStatus == CredentialStatus.REVOKED) {
            credential.setRevokedAt(Instant.now());
        }

        credential = storedCredentialRepository.save(credential);
        return mapStoredCredentialToDto(credential);
    }

    // ==================== Helper Methods ====================

    /**
     * Extract credential type from JSON.
     */
    private String extractCredentialType(com.fasterxml.jackson.databind.JsonNode credentialJson) {
        if (credentialJson.has("type") && credentialJson.get("type").isArray()) {
            var types = credentialJson.get("type");
            for (int i = 0; i < types.size(); i++) {
                String type = types.get(i).asText();
                if (!"VerifiableCredential".equals(type)) {
                    return type;
                }
            }
        }
        return "VerifiableCredential";
    }

    /**
     * Extract issuer DID from JSON.
     */
    private String extractIssuerDid(com.fasterxml.jackson.databind.JsonNode credentialJson) {
        if (credentialJson.has("issuer")) {
            var issuer = credentialJson.get("issuer");
            if (issuer.isTextual()) {
                return issuer.asText();
            } else if (issuer.isObject() && issuer.has("id")) {
                return issuer.get("id").asText();
            }
        }
        return "unknown";
    }

    /**
     * Extract issuer name from JSON.
     */
    private String extractIssuerName(com.fasterxml.jackson.databind.JsonNode credentialJson) {
        if (credentialJson.has("issuer")) {
            var issuer = credentialJson.get("issuer");
            if (issuer.isObject() && issuer.has("name")) {
                return issuer.get("name").asText();
            }
        }
        return null;
    }

    /**
     * Extract subject DID from JSON.
     */
    private String extractSubjectDid(com.fasterxml.jackson.databind.JsonNode credentialJson) {
        if (credentialJson.has("credentialSubject")) {
            var subject = credentialJson.get("credentialSubject");
            if (subject.has("id")) {
                return subject.get("id").asText();
            }
        }
        return "unknown";
    }

    /**
     * Extract issuance date from JSON.
     */
    private Instant extractIssuedAt(com.fasterxml.jackson.databind.JsonNode credentialJson) {
        if (credentialJson.has("issuanceDate")) {
            try {
                return Instant.parse(credentialJson.get("issuanceDate").asText());
            } catch (Exception e) {
                log.warn("Failed to parse issuanceDate", e);
            }
        }
        return Instant.now();
    }

    /**
     * Extract expiration date from JSON.
     */
    private Instant extractExpiresAt(com.fasterxml.jackson.databind.JsonNode credentialJson) {
        if (credentialJson.has("expirationDate")) {
            try {
                return Instant.parse(credentialJson.get("expirationDate").asText());
            } catch (Exception e) {
                log.warn("Failed to parse expirationDate", e);
            }
        }
        return null;
    }

    /**
     * Map StoredCredential to DTO.
     */
    private CredentialDto mapStoredCredentialToDto(StoredCredential credential) {
        boolean expired = credential.getExpiresAt() != null && credential.getExpiresAt().isBefore(Instant.now());
        boolean revoked = credential.getStatus() == CredentialStatus.REVOKED;

        return CredentialDto.builder()
            .id(credential.getId())
            .credentialId(credential.getCredentialId())
            .credentialType(credential.getCredentialType())
            .issuerDid(credential.getIssuerDid())
            .issuerName(credential.getIssuerName())
            .subjectDid(credential.getSubjectDid())
            .credentialData(credential.getCredentialData())
            .status(credential.getStatus())
            .issuedAt(credential.getIssuedAt())
            .expiresAt(credential.getExpiresAt())
            .expired(expired)
            .revoked(revoked)
            .revokedAt(credential.getRevokedAt())
            .revocationReason(credential.getRevocationReason())
            .verified(credential.isVerified())
            .lastVerifiedAt(credential.getLastVerifiedAt())
            .tags(credential.getTags())
            .metadata(credential.getMetadata())
            .createdAt(credential.getCreatedAt())
            .updatedAt(credential.getUpdatedAt())
            .build();
    }

    /**
     * Map CredentialMetadata to DTO.
     */
    private CredentialDto mapCredentialMetadataToDto(CredentialMetadata metadata) {
        boolean expired = metadata.getExpiresAt() != null && metadata.getExpiresAt().isBefore(Instant.now());

        return CredentialDto.builder()
            .id(metadata.getId())
            .credentialId(metadata.getCredentialId())
            .credentialType(metadata.getCredentialType())
            .issuerDid(metadata.getIssuerDid())
            .issuerName(metadata.getIssuerName())
            .subjectDid(metadata.getSubjectDid())
            .credentialHash(metadata.getCredentialHash())
            .storageLocation(metadata.getStorageLocation())
            .issuedAt(metadata.getIssuedAt())
            .expiresAt(metadata.getExpiresAt())
            .expired(expired)
            .tags(metadata.getTags())
            .metadata(metadata.getMetadata())
            .createdAt(metadata.getCreatedAt())
            .updatedAt(metadata.getUpdatedAt())
            .build();
    }
}

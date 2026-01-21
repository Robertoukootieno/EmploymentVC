package io.provenly.wallet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.provenly.commons.exception.ProvenlyException;
import io.provenly.crypto.model.KeyType;
import io.provenly.crypto.service.EncryptionService;
import io.provenly.crypto.service.SigningService;
import io.provenly.wallet.dto.CreatePresentationRequest;
import io.provenly.wallet.dto.PresentationDto;
import io.provenly.wallet.dto.SelectiveDisclosureDto;
import io.provenly.wallet.model.CustodialWallet;
import io.provenly.wallet.model.StoredCredential;
import io.provenly.wallet.repository.CustodialWalletRepository;
import io.provenly.wallet.repository.StoredCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for creating verifiable presentations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PresentationService {

    private final CustodialWalletRepository custodialWalletRepository;
    private final StoredCredentialRepository storedCredentialRepository;
    private final SigningService signingService;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    @Value("${wallet.encryption.password:change-this-in-production}")
    private String encryptionPassword;

    /**
     * Create a verifiable presentation from selected credentials.
     */
    @Transactional
    public PresentationDto createPresentation(UUID walletId, UUID userId, CreatePresentationRequest request) {
        log.info("Creating presentation for wallet: {} with {} credentials", walletId, request.getCredentialIds().size());

        try {
            // Verify wallet exists and belongs to user
            CustodialWallet wallet = custodialWalletRepository.findById(walletId)
                .filter(w -> w.getUserId().equals(userId))
                .orElseThrow(() -> new ProvenlyException.NotFoundException("Custodial wallet not found"));

            // Retrieve credentials
            List<StoredCredential> credentials = new ArrayList<>();
            for (String credentialId : request.getCredentialIds()) {
                StoredCredential credential = storedCredentialRepository.findByWalletIdAndCredentialId(walletId, credentialId)
                    .orElseThrow(() -> new ProvenlyException.NotFoundException("Credential not found: " + credentialId));
                credentials.add(credential);
            }

            // Apply selective disclosure if requested
            List<String> processedCredentials = credentials.stream()
                .map(c -> applySelectiveDisclosure(c, request.getSelectiveDisclosure()))
                .collect(Collectors.toList());

            // Create presentation
            ObjectNode presentation = objectMapper.createObjectNode();
            presentation.put("@context", objectMapper.createArrayNode()
                .add("https://www.w3.org/2018/credentials/v1"));
            presentation.put("id", "urn:uuid:" + UUID.randomUUID());
            presentation.put("type", objectMapper.createArrayNode()
                .add("VerifiablePresentation"));
            presentation.put("holder", wallet.getDid());

            // Add credentials
            ArrayNode credentialsArray = objectMapper.createArrayNode();
            for (String credentialData : processedCredentials) {
                credentialsArray.add(objectMapper.readTree(credentialData));
            }
            presentation.set("verifiableCredential", credentialsArray);

            // Create proof
            ObjectNode proof = createProof(wallet, request.getChallenge(), request.getDomain(), presentation);
            presentation.set("proof", proof);

            String presentationData = objectMapper.writeValueAsString(presentation);

            log.info("Created presentation with {} credentials", credentials.size());

            return PresentationDto.builder()
                .id(presentation.get("id").asText())
                .type("VerifiablePresentation")
                .holder(wallet.getDid())
                .presentationData(presentationData)
                .proof(objectMapper.writeValueAsString(proof))
                .createdAt(Instant.now())
                .build();

        } catch (ProvenlyException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create presentation", e);
            throw new ProvenlyException.InternalServerException("Failed to create presentation: " + e.getMessage());
        }
    }

    /**
     * Verify a presentation (basic verification).
     */
    @Transactional(readOnly = true)
    public boolean verifyPresentation(String presentationData) {
        log.info("Verifying presentation");

        try {
            var presentation = objectMapper.readTree(presentationData);

            // Check required fields
            if (!presentation.has("holder") || !presentation.has("proof") || !presentation.has("verifiableCredential")) {
                log.warn("Presentation missing required fields");
                return false;
            }

            // TODO: Implement full verification logic
            // - Verify proof signature
            // - Verify each credential in the presentation
            // - Check expiration dates
            // - Verify issuer signatures

            log.info("Presentation verification completed");
            return true;

        } catch (Exception e) {
            log.error("Failed to verify presentation", e);
            return false;
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Apply selective disclosure to a credential.
     */
    private String applySelectiveDisclosure(StoredCredential credential, List<SelectiveDisclosureDto> selectiveDisclosureConfig) {
        if (selectiveDisclosureConfig == null || selectiveDisclosureConfig.isEmpty()) {
            return credential.getCredentialData();
        }

        try {
            // Find selective disclosure config for this credential
            Optional<SelectiveDisclosureDto> config = selectiveDisclosureConfig.stream()
                .filter(sd -> sd.getCredentialId().equals(credential.getCredentialId()))
                .findFirst();

            if (config.isEmpty()) {
                return credential.getCredentialData();
            }

            SelectiveDisclosureDto sdConfig = config.get();
            var credentialJson = objectMapper.readTree(credential.getCredentialData());

            // If disclosed fields are specified, only include those
            if (sdConfig.getDisclosedFields() != null && !sdConfig.getDisclosedFields().isEmpty()) {
                ObjectNode filteredCredential = objectMapper.createObjectNode();

                // Always include required fields
                if (credentialJson.has("@context")) filteredCredential.set("@context", credentialJson.get("@context"));
                if (credentialJson.has("id")) filteredCredential.set("id", credentialJson.get("id"));
                if (credentialJson.has("type")) filteredCredential.set("type", credentialJson.get("type"));
                if (credentialJson.has("issuer")) filteredCredential.set("issuer", credentialJson.get("issuer"));
                if (credentialJson.has("issuanceDate")) filteredCredential.set("issuanceDate", credentialJson.get("issuanceDate"));
                if (credentialJson.has("proof")) filteredCredential.set("proof", credentialJson.get("proof"));

                // Add disclosed fields from credentialSubject
                if (credentialJson.has("credentialSubject")) {
                    var subject = credentialJson.get("credentialSubject");
                    ObjectNode filteredSubject = objectMapper.createObjectNode();

                    if (subject.has("id")) filteredSubject.set("id", subject.get("id"));

                    for (String field : sdConfig.getDisclosedFields()) {
                        if (subject.has(field)) {
                            filteredSubject.set(field, subject.get(field));
                        }
                    }

                    filteredCredential.set("credentialSubject", filteredSubject);
                }

                return objectMapper.writeValueAsString(filteredCredential);
            }

            // If hidden fields are specified, remove those
            if (sdConfig.getHiddenFields() != null && !sdConfig.getHiddenFields().isEmpty()) {
                ObjectNode modifiedCredential = (ObjectNode) credentialJson;

                if (modifiedCredential.has("credentialSubject")) {
                    ObjectNode subject = (ObjectNode) modifiedCredential.get("credentialSubject");
                    for (String field : sdConfig.getHiddenFields()) {
                        subject.remove(field);
                    }
                }

                return objectMapper.writeValueAsString(modifiedCredential);
            }

            return credential.getCredentialData();

        } catch (Exception e) {
            log.error("Failed to apply selective disclosure", e);
            return credential.getCredentialData();
        }
    }

    /**
     * Create proof for the presentation.
     */
    private ObjectNode createProof(CustodialWallet wallet, String challenge, String domain, ObjectNode presentation) {
        try {
            // Decrypt private key
            byte[] privateKeyBytes = encryptionService.decryptWithPassword(
                wallet.getEncryptedPrivateKey(),
                encryptionPassword
            );

            // Create proof object
            ObjectNode proof = objectMapper.createObjectNode();
            proof.put("type", getProofType(wallet.getKeyType()));
            proof.put("created", Instant.now().toString());
            proof.put("verificationMethod", wallet.getDid() + "#key-1");
            proof.put("proofPurpose", "authentication");
            proof.put("challenge", challenge);
            if (domain != null) {
                proof.put("domain", domain);
            }

            // Create data to sign (presentation without proof)
            String dataToSign = objectMapper.writeValueAsString(presentation);

            // Sign the data
            KeyType keyType = KeyType.valueOf(wallet.getKeyType());
            String signature = signingService.sign(dataToSign.getBytes(), privateKeyBytes, keyType);
            proof.put("jws", signature);

            return proof;

        } catch (Exception e) {
            log.error("Failed to create proof", e);
            throw new ProvenlyException.InternalServerException("Failed to create proof: " + e.getMessage());
        }
    }

    /**
     * Get proof type based on key type.
     */
    private String getProofType(String keyType) {
        return switch (keyType) {
            case "Ed25519" -> "Ed25519Signature2020";
            case "SECP256K1" -> "EcdsaSecp256k1Signature2019";
            case "SECP256R1" -> "EcdsaSecp256r1Signature2019";
            case "RSA" -> "RsaSignature2018";
            default -> "JsonWebSignature2020";
        };
    }
}

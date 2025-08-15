package io.provenly.application.issuer.service;

import io.provenly.application.domain.model.VerifiableCredential;
import io.provenly.application.issuer.dto.IssueCredentialRequest;
import io.provenly.application.issuer.dto.IssueCredentialResponse;
import io.provenly.application.issuer.dto.RevokeCredentialRequest;
import io.provenly.application.common.exception.CredentialIssuanceException;
import io.provenly.application.common.exception.CredentialRevocationException;
import io.provenly.application.config.ApplicationConfig;
import io.provenly.application.repository.VerifiableCredentialRepository;
import io.provenly.application.external.WaltIdService;
import io.provenly.application.external.EbsiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for issuing Verifiable Credentials.
 * Integrates with walt.id for VC processing and EBSI for DID operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CredentialIssuerService {

    private final VerifiableCredentialRepository credentialRepository;
    private final WaltIdService waltIdService;
    private final EbsiService ebsiService;
    private final SelectiveDisclosureService selectiveDisclosureService;
    private final RevocationRegistryService revocationRegistryService;
    private final ApplicationConfig.WaltIdProperties waltIdProperties;
    private final ApplicationConfig.SelectiveDisclosureProperties sdProperties;

    /**
     * Issue a new Verifiable Credential.
     */
    @Transactional
    public IssueCredentialResponse issueCredential(IssueCredentialRequest request) {
        try {
            log.info("Issuing credential for subject: {}", request.getSubjectDid());

            // Validate the request
            validateIssueRequest(request);

            // Prepare credential data
            Map<String, Object> credentialData = prepareCredentialData(request);

            // Issue credential using walt.id
            Map<String, Object> issuedCredential = waltIdService.issueCredential(credentialData);

            // Handle selective disclosure if enabled
            Map<String, Object> selectiveDisclosureData = null;
            if (request.isSelectiveDisclosureEnabled()) {
                selectiveDisclosureData = selectiveDisclosureService.enableSelectiveDisclosure(
                    issuedCredential, request.getSelectiveDisclosureConfig()
                );
            }

            // Register in revocation registry if needed
            String revocationRegistryId = null;
            Long revocationIndex = null;
            if (request.isRevocable()) {
                var revocationInfo = revocationRegistryService.registerCredential(
                    request.getIssuerDid(), 
                    (String) issuedCredential.get("id")
                );
                revocationRegistryId = revocationInfo.getRegistryId();
                revocationIndex = revocationInfo.getIndex();
            }

            // Save to database
            VerifiableCredential credential = VerifiableCredential.builder()
                .credentialId((String) issuedCredential.get("id"))
                .context((List<String>) issuedCredential.get("@context"))
                .type((List<String>) issuedCredential.get("type"))
                .issuer(request.getIssuerDid())
                .issuanceDate(Instant.now())
                .expirationDate(request.getExpirationDate())
                .credentialSubjectId(request.getSubjectDid())
                .credentialSubject(request.getCredentialData())
                .proof((Map<String, Object>) issuedCredential.get("proof"))
                .schemaId(request.getSchemaId())
                .selectiveDisclosureEnabled(request.isSelectiveDisclosureEnabled())
                .selectiveDisclosureData(selectiveDisclosureData)
                .revocationRegistryId(revocationRegistryId)
                .revocationIndex(revocationIndex)
                .metadata(request.getMetadata())
                .build();

            credential = credentialRepository.save(credential);

            log.info("Successfully issued credential with ID: {}", credential.getCredentialId());

            return IssueCredentialResponse.builder()
                .credentialId(credential.getCredentialId())
                .credential(issuedCredential)
                .selectiveDisclosureData(selectiveDisclosureData)
                .revocationRegistryId(revocationRegistryId)
                .revocationIndex(revocationIndex)
                .issuanceDate(credential.getIssuanceDate())
                .build();

        } catch (Exception e) {
            log.error("Failed to issue credential for subject: {}", request.getSubjectDid(), e);
            throw new CredentialIssuanceException("Failed to issue credential", e);
        }
    }

    /**
     * Revoke a Verifiable Credential.
     */
    @Transactional
    public void revokeCredential(RevokeCredentialRequest request) {
        try {
            log.info("Revoking credential: {}", request.getCredentialId());

            // Find the credential
            VerifiableCredential credential = credentialRepository
                .findByCredentialId(request.getCredentialId())
                .orElseThrow(() -> new CredentialRevocationException("Credential not found"));

            // Verify issuer authorization
            if (!credential.getIssuer().equals(request.getIssuerDid())) {
                throw new CredentialRevocationException("Unauthorized to revoke this credential");
            }

            // Update revocation registry
            if (credential.getRevocationRegistryId() != null) {
                revocationRegistryService.revokeCredential(
                    credential.getRevocationRegistryId(),
                    credential.getRevocationIndex(),
                    request.getReason()
                );
            }

            // Update credential status
            credential.setStatus(VerifiableCredential.CredentialStatus.REVOKED);
            credentialRepository.save(credential);

            log.info("Successfully revoked credential: {}", request.getCredentialId());

        } catch (Exception e) {
            log.error("Failed to revoke credential: {}", request.getCredentialId(), e);
            throw new CredentialRevocationException("Failed to revoke credential", e);
        }
    }

    /**
     * Get issued credentials for an issuer.
     */
    public List<VerifiableCredential> getIssuedCredentials(String issuerDid, int page, int size) {
        return credentialRepository.findByIssuerOrderByCreatedAtDesc(issuerDid, 
            org.springframework.data.domain.PageRequest.of(page, size)).getContent();
    }

    /**
     * Get credential by ID.
     */
    public VerifiableCredential getCredential(String credentialId) {
        return credentialRepository.findByCredentialId(credentialId)
            .orElseThrow(() -> new CredentialIssuanceException("Credential not found"));
    }

    /**
     * Check credential status.
     */
    public VerifiableCredential.CredentialStatus getCredentialStatus(String credentialId) {
        return credentialRepository.findByCredentialId(credentialId)
            .map(VerifiableCredential::getStatus)
            .orElseThrow(() -> new CredentialIssuanceException("Credential not found"));
    }

    /**
     * Validate the issue credential request.
     */
    private void validateIssueRequest(IssueCredentialRequest request) {
        if (request.getIssuerDid() == null || request.getIssuerDid().trim().isEmpty()) {
            throw new CredentialIssuanceException("Issuer DID is required");
        }
        if (request.getSubjectDid() == null || request.getSubjectDid().trim().isEmpty()) {
            throw new CredentialIssuanceException("Subject DID is required");
        }
        if (request.getCredentialData() == null || request.getCredentialData().isEmpty()) {
            throw new CredentialIssuanceException("Credential data is required");
        }
        if (request.getSchemaId() == null || request.getSchemaId().trim().isEmpty()) {
            throw new CredentialIssuanceException("Schema ID is required");
        }
    }

    /**
     * Prepare credential data for walt.id.
     */
    private Map<String, Object> prepareCredentialData(IssueCredentialRequest request) {
        return Map.of(
            "@context", List.of(
                "https://www.w3.org/2018/credentials/v1",
                "https://provenly.io/contexts/employment/v1"
            ),
            "type", List.of("VerifiableCredential", "EmploymentCredential"),
            "issuer", request.getIssuerDid(),
            "issuanceDate", Instant.now().toString(),
            "expirationDate", request.getExpirationDate() != null ? 
                request.getExpirationDate().toString() : null,
            "credentialSubject", Map.of(
                "id", request.getSubjectDid(),
                "data", request.getCredentialData()
            ),
            "credentialSchema", Map.of(
                "id", request.getSchemaId(),
                "type", "JsonSchemaValidator2018"
            )
        );
    }
}

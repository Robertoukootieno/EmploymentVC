package io.provenly.application.verifier.service;

import io.provenly.application.verifier.dto.VerifyCredentialRequest;
import io.provenly.application.verifier.dto.VerifyCredentialResponse;
import io.provenly.application.verifier.dto.VerifyPresentationRequest;
import io.provenly.application.verifier.dto.VerifyPresentationResponse;
import io.provenly.application.verifier.dto.VerificationResult;
import io.provenly.application.common.exception.CredentialVerificationException;
import io.provenly.application.external.WaltIdService;
import io.provenly.application.external.EbsiService;
import io.provenly.application.issuer.service.RevocationRegistryService;
import io.provenly.application.issuer.service.SelectiveDisclosureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Service for verifying Verifiable Credentials and Presentations.
 * Supports selective disclosure verification and revocation checking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CredentialVerifierService {

    private final WaltIdService waltIdService;
    private final EbsiService ebsiService;
    private final RevocationRegistryService revocationRegistryService;
    private final SelectiveDisclosureService selectiveDisclosureService;

    /**
     * Verify a Verifiable Credential.
     */
    public VerifyCredentialResponse verifyCredential(VerifyCredentialRequest request) {
        try {
            log.info("Verifying credential: {}", request.getCredential().get("id"));

            List<VerificationResult> results = new ArrayList<>();
            boolean overallValid = true;

            // 1. Verify cryptographic proof
            VerificationResult proofResult = verifyProof(request.getCredential());
            results.add(proofResult);
            if (!proofResult.isValid()) {
                overallValid = false;
            }

            // 2. Verify issuer DID
            VerificationResult issuerResult = verifyIssuer(request.getCredential());
            results.add(issuerResult);
            if (!issuerResult.isValid()) {
                overallValid = false;
            }

            // 3. Check expiration
            if (request.getOptions().isCheckExpiration()) {
                VerificationResult expirationResult = verifyExpiration(request.getCredential());
                results.add(expirationResult);
                if (!expirationResult.isValid()) {
                    overallValid = false;
                }
            }

            // 4. Check revocation status
            if (request.getOptions().isCheckRevocation()) {
                VerificationResult revocationResult = verifyRevocationStatus(request.getCredential());
                results.add(revocationResult);
                if (!revocationResult.isValid()) {
                    overallValid = false;
                }
            }

            // 5. Verify schema compliance
            if (request.getOptions().isCheckSchema()) {
                VerificationResult schemaResult = verifySchema(request.getCredential());
                results.add(schemaResult);
                if (!schemaResult.isValid()) {
                    overallValid = false;
                }
            }

            // 6. Verify selective disclosure if present
            if (request.getCredential().containsKey("selectiveDisclosure")) {
                VerificationResult sdResult = verifySelectiveDisclosure(request.getCredential());
                results.add(sdResult);
                if (!sdResult.isValid()) {
                    overallValid = false;
                }
            }

            log.info("Credential verification completed. Valid: {}", overallValid);

            return VerifyCredentialResponse.builder()
                .valid(overallValid)
                .results(results)
                .verificationDate(Instant.now())
                .credentialId((String) request.getCredential().get("id"))
                .build();

        } catch (Exception e) {
            log.error("Failed to verify credential", e);
            throw new CredentialVerificationException("Failed to verify credential", e);
        }
    }

    /**
     * Verify a Verifiable Presentation.
     */
    public VerifyPresentationResponse verifyPresentation(VerifyPresentationRequest request) {
        try {
            log.info("Verifying presentation from holder: {}", request.getPresentation().get("holder"));

            List<VerificationResult> results = new ArrayList<>();
            boolean overallValid = true;

            // 1. Verify presentation proof
            VerificationResult presentationProofResult = verifyPresentationProof(
                request.getPresentation(), 
                request.getChallenge(), 
                request.getDomain()
            );
            results.add(presentationProofResult);
            if (!presentationProofResult.isValid()) {
                overallValid = false;
            }

            // 2. Verify holder DID
            VerificationResult holderResult = verifyHolder(request.getPresentation());
            results.add(holderResult);
            if (!holderResult.isValid()) {
                overallValid = false;
            }

            // 3. Verify each credential in the presentation
            List<Map<String, Object>> credentials = (List<Map<String, Object>>) 
                request.getPresentation().get("verifiableCredential");
            
            if (credentials != null) {
                for (int i = 0; i < credentials.size(); i++) {
                    Map<String, Object> credential = credentials.get(i);
                    
                    VerifyCredentialRequest credentialRequest = VerifyCredentialRequest.builder()
                        .credential(credential)
                        .options(request.getOptions())
                        .build();
                    
                    VerifyCredentialResponse credentialResponse = verifyCredential(credentialRequest);
                    
                    VerificationResult credentialResult = VerificationResult.builder()
                        .type("credential_" + i)
                        .valid(credentialResponse.isValid())
                        .message(credentialResponse.isValid() ? 
                            "Credential is valid" : "Credential verification failed")
                        .details(Map.of("credentialResults", credentialResponse.getResults()))
                        .build();
                    
                    results.add(credentialResult);
                    if (!credentialResult.isValid()) {
                        overallValid = false;
                    }
                }
            }

            log.info("Presentation verification completed. Valid: {}", overallValid);

            return VerifyPresentationResponse.builder()
                .valid(overallValid)
                .results(results)
                .verificationDate(Instant.now())
                .holder((String) request.getPresentation().get("holder"))
                .build();

        } catch (Exception e) {
            log.error("Failed to verify presentation", e);
            throw new CredentialVerificationException("Failed to verify presentation", e);
        }
    }

    /**
     * Verify cryptographic proof of a credential.
     */
    private VerificationResult verifyProof(Map<String, Object> credential) {
        try {
            boolean isValid = waltIdService.verifyCredentialProof(credential);
            
            return VerificationResult.builder()
                .type("proof")
                .valid(isValid)
                .message(isValid ? "Cryptographic proof is valid" : "Invalid cryptographic proof")
                .details(Map.of("proofType", ((Map<String, Object>) credential.get("proof")).get("type")))
                .build();
        } catch (Exception e) {
            log.error("Failed to verify proof", e);
            return VerificationResult.builder()
                .type("proof")
                .valid(false)
                .message("Proof verification failed: " + e.getMessage())
                .build();
        }
    }

    /**
     * Verify issuer DID and authorization.
     */
    private VerificationResult verifyIssuer(Map<String, Object> credential) {
        try {
            String issuer = (String) credential.get("issuer");
            boolean isValid = ebsiService.verifyIssuerDid(issuer);
            
            return VerificationResult.builder()
                .type("issuer")
                .valid(isValid)
                .message(isValid ? "Issuer DID is valid" : "Invalid or unauthorized issuer")
                .details(Map.of("issuer", issuer))
                .build();
        } catch (Exception e) {
            log.error("Failed to verify issuer", e);
            return VerificationResult.builder()
                .type("issuer")
                .valid(false)
                .message("Issuer verification failed: " + e.getMessage())
                .build();
        }
    }

    /**
     * Verify credential expiration.
     */
    private VerificationResult verifyExpiration(Map<String, Object> credential) {
        try {
            String expirationDateStr = (String) credential.get("expirationDate");
            if (expirationDateStr == null) {
                return VerificationResult.builder()
                    .type("expiration")
                    .valid(true)
                    .message("No expiration date set")
                    .build();
            }

            Instant expirationDate = Instant.parse(expirationDateStr);
            boolean isValid = Instant.now().isBefore(expirationDate);
            
            return VerificationResult.builder()
                .type("expiration")
                .valid(isValid)
                .message(isValid ? "Credential is not expired" : "Credential has expired")
                .details(Map.of("expirationDate", expirationDateStr))
                .build();
        } catch (Exception e) {
            log.error("Failed to verify expiration", e);
            return VerificationResult.builder()
                .type("expiration")
                .valid(false)
                .message("Expiration verification failed: " + e.getMessage())
                .build();
        }
    }

    /**
     * Verify revocation status.
     */
    private VerificationResult verifyRevocationStatus(Map<String, Object> credential) {
        try {
            String credentialId = (String) credential.get("id");
            boolean isRevoked = revocationRegistryService.isCredentialRevoked(credentialId);
            
            return VerificationResult.builder()
                .type("revocation")
                .valid(!isRevoked)
                .message(isRevoked ? "Credential has been revoked" : "Credential is not revoked")
                .details(Map.of("credentialId", credentialId))
                .build();
        } catch (Exception e) {
            log.error("Failed to verify revocation status", e);
            return VerificationResult.builder()
                .type("revocation")
                .valid(false)
                .message("Revocation verification failed: " + e.getMessage())
                .build();
        }
    }

    /**
     * Verify schema compliance.
     */
    private VerificationResult verifySchema(Map<String, Object> credential) {
        try {
            // Extract schema information
            Map<String, Object> credentialSchema = (Map<String, Object>) credential.get("credentialSchema");
            if (credentialSchema == null) {
                return VerificationResult.builder()
                    .type("schema")
                    .valid(true)
                    .message("No schema specified")
                    .build();
            }

            String schemaId = (String) credentialSchema.get("id");
            boolean isValid = waltIdService.validateCredentialSchema(credential, schemaId);
            
            return VerificationResult.builder()
                .type("schema")
                .valid(isValid)
                .message(isValid ? "Credential complies with schema" : "Schema validation failed")
                .details(Map.of("schemaId", schemaId))
                .build();
        } catch (Exception e) {
            log.error("Failed to verify schema", e);
            return VerificationResult.builder()
                .type("schema")
                .valid(false)
                .message("Schema verification failed: " + e.getMessage())
                .build();
        }
    }

    /**
     * Verify selective disclosure.
     */
    private VerificationResult verifySelectiveDisclosure(Map<String, Object> credential) {
        try {
            boolean isValid = selectiveDisclosureService.verifySelectiveDisclosure(credential);
            
            return VerificationResult.builder()
                .type("selective_disclosure")
                .valid(isValid)
                .message(isValid ? "Selective disclosure is valid" : "Invalid selective disclosure")
                .build();
        } catch (Exception e) {
            log.error("Failed to verify selective disclosure", e);
            return VerificationResult.builder()
                .type("selective_disclosure")
                .valid(false)
                .message("Selective disclosure verification failed: " + e.getMessage())
                .build();
        }
    }

    /**
     * Verify presentation proof.
     */
    private VerificationResult verifyPresentationProof(
            Map<String, Object> presentation, 
            String challenge, 
            String domain) {
        try {
            boolean isValid = waltIdService.verifyPresentationProof(presentation, challenge, domain);
            
            return VerificationResult.builder()
                .type("presentation_proof")
                .valid(isValid)
                .message(isValid ? "Presentation proof is valid" : "Invalid presentation proof")
                .details(Map.of("challenge", challenge, "domain", domain))
                .build();
        } catch (Exception e) {
            log.error("Failed to verify presentation proof", e);
            return VerificationResult.builder()
                .type("presentation_proof")
                .valid(false)
                .message("Presentation proof verification failed: " + e.getMessage())
                .build();
        }
    }

    /**
     * Verify holder DID.
     */
    private VerificationResult verifyHolder(Map<String, Object> presentation) {
        try {
            String holder = (String) presentation.get("holder");
            boolean isValid = ebsiService.verifyHolderDid(holder);
            
            return VerificationResult.builder()
                .type("holder")
                .valid(isValid)
                .message(isValid ? "Holder DID is valid" : "Invalid holder DID")
                .details(Map.of("holder", holder))
                .build();
        } catch (Exception e) {
            log.error("Failed to verify holder", e);
            return VerificationResult.builder()
                .type("holder")
                .valid(false)
                .message("Holder verification failed: " + e.getMessage())
                .build();
        }
    }
}

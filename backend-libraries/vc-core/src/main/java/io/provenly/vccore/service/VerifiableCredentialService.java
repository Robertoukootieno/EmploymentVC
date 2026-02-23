package io.provenly.vccore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.provenly.vccore.model.VerifiableCredential;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core creation and validation operations for verifiable credentials.
 */
public class VerifiableCredentialService {

    private static final String W3C_VC_CONTEXT = "https://www.w3.org/2018/credentials/v1";

    private final ObjectMapper objectMapper;

    public VerifiableCredentialService() {
        this(new ObjectMapper());
    }

    public VerifiableCredentialService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public VerifiableCredential createEmploymentCredential(
            String issuerDid,
            String subjectDid,
            Map<String, Object> employmentClaims,
            long validityDays
    ) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(validityDays, ChronoUnit.DAYS);

        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("id", subjectDid);
        subject.putAll(employmentClaims);

        VerifiableCredential credential = new VerifiableCredential();
        credential.setContext(List.of(W3C_VC_CONTEXT));
        credential.setType(List.of("VerifiableCredential", "EmploymentCredential"));
        credential.setId("urn:uuid:" + UUID.randomUUID());
        credential.setIssuer(issuerDid);
        credential.setIssuanceDate(issuedAt);
        credential.setExpirationDate(expiresAt);
        credential.setCredentialSubject(subject);
        return credential;
    }

    public List<String> validate(VerifiableCredential credential) {
        List<String> issues = new ArrayList<>();

        if (credential == null) {
            issues.add("Credential is null");
            return issues;
        }

        if (credential.getContext() == null || credential.getContext().isEmpty()) {
            issues.add("@context is required");
        }

        if (credential.getType() == null || credential.getType().isEmpty()) {
            issues.add("type is required");
        }

        if (isBlank(credential.getId())) {
            issues.add("id is required");
        }

        if (isBlank(credential.getIssuer())) {
            issues.add("issuer is required");
        }

        if (credential.getIssuanceDate() == null) {
            issues.add("issuanceDate is required");
        }

        if (credential.getCredentialSubject() == null || credential.getCredentialSubject().isEmpty()) {
            issues.add("credentialSubject is required");
        }

        if (credential.getIssuanceDate() != null && credential.getExpirationDate() != null
                && credential.getExpirationDate().isBefore(credential.getIssuanceDate())) {
            issues.add("expirationDate cannot be before issuanceDate");
        }

        return issues;
    }

    public boolean isExpired(VerifiableCredential credential) {
        return credential != null
                && credential.getExpirationDate() != null
                && credential.getExpirationDate().isBefore(Instant.now());
    }

    public String toJson(VerifiableCredential credential) {
        try {
            return objectMapper.writeValueAsString(credential);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to serialize verifiable credential", exception);
        }
    }

    public VerifiableCredential fromJson(String json) {
        try {
            return objectMapper.readValue(json, VerifiableCredential.class);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to deserialize verifiable credential", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

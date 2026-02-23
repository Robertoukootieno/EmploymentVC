package io.provenly.vccore.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal W3C Verifiable Credential representation.
 */
public class VerifiableCredential {

    @JsonProperty("@context")
    private List<String> context = new ArrayList<>();

    private List<String> type = new ArrayList<>();
    private String id;
    private String issuer;
    private Instant issuanceDate;
    private Instant expirationDate;
    private Map<String, Object> credentialSubject = new LinkedHashMap<>();
    private Map<String, Object> proof = new LinkedHashMap<>();

    public List<String> getContext() {
        return context;
    }

    public void setContext(List<String> context) {
        this.context = context;
    }

    public List<String> getType() {
        return type;
    }

    public void setType(List<String> type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Instant getIssuanceDate() {
        return issuanceDate;
    }

    public void setIssuanceDate(Instant issuanceDate) {
        this.issuanceDate = issuanceDate;
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Instant expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Map<String, Object> getCredentialSubject() {
        return credentialSubject;
    }

    public void setCredentialSubject(Map<String, Object> credentialSubject) {
        this.credentialSubject = credentialSubject;
    }

    public Map<String, Object> getProof() {
        return proof;
    }

    public void setProof(Map<String, Object> proof) {
        this.proof = proof;
    }
}

package io.provenly.credentialslib.model;

import io.provenly.vccore.model.VerifiableCredential;

import java.time.Instant;

/**
 * Stored credential metadata and payload.
 */
public class CredentialRecord {

    private String id;
    private String holderDid;
    private String issuerDid;
    private String type;
    private Instant createdAt;
    private VerifiableCredential credential;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHolderDid() {
        return holderDid;
    }

    public void setHolderDid(String holderDid) {
        this.holderDid = holderDid;
    }

    public String getIssuerDid() {
        return issuerDid;
    }

    public void setIssuerDid(String issuerDid) {
        this.issuerDid = issuerDid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public VerifiableCredential getCredential() {
        return credential;
    }

    public void setCredential(VerifiableCredential credential) {
        this.credential = credential;
    }
}

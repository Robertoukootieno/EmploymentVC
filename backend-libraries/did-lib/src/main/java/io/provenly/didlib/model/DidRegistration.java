package io.provenly.didlib.model;

import io.provenly.didcore.model.DidDocument;

import java.time.Instant;

/**
 * Registry record for a DID and its current document snapshot.
 */
public class DidRegistration {

    private String did;
    private String ownerId;
    private Instant registeredAt;
    private DidDocument document;

    public String getDid() {
        return did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public DidDocument getDocument() {
        return document;
    }

    public void setDocument(DidDocument document) {
        this.document = document;
    }
}

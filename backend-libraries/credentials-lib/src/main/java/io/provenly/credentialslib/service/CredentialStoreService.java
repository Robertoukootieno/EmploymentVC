package io.provenly.credentialslib.service;

import io.provenly.credentialslib.model.CredentialRecord;
import io.provenly.vccore.model.VerifiableCredential;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory credential store abstraction for service-layer integration.
 */
public class CredentialStoreService {

    private final Map<String, CredentialRecord> store = new ConcurrentHashMap<>();

    public CredentialRecord save(String holderDid, VerifiableCredential credential) {
        String id = credential.getId() != null ? credential.getId() : "urn:uuid:" + UUID.randomUUID();

        CredentialRecord record = new CredentialRecord();
        record.setId(id);
        record.setHolderDid(holderDid);
        record.setIssuerDid(credential.getIssuer());
        record.setType(firstType(credential));
        record.setCreatedAt(Instant.now());
        record.setCredential(credential);

        store.put(id, record);
        return record;
    }

    public CredentialRecord findById(String credentialId) {
        return store.get(credentialId);
    }

    public List<CredentialRecord> findByHolderDid(String holderDid) {
        return store.values().stream()
                .filter(record -> holderDid.equals(record.getHolderDid()))
                .toList();
    }

    public List<CredentialRecord> findAll() {
        return new ArrayList<>(store.values());
    }

    public boolean deleteById(String credentialId) {
        return store.remove(credentialId) != null;
    }

    private String firstType(VerifiableCredential credential) {
        if (credential.getType() == null || credential.getType().isEmpty()) {
            return "VerifiableCredential";
        }
        return credential.getType().get(credential.getType().size() - 1);
    }
}

package io.provenly.didlib.service;

import io.provenly.didcore.model.DidDocument;
import io.provenly.didcore.service.DidService;
import io.provenly.didlib.model.DidRegistration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory DID registry helper built on top of did-core.
 */
public class DidRegistryService {

    private final DidService didService;
    private final Map<String, DidRegistration> registrations = new ConcurrentHashMap<>();

    public DidRegistryService() {
        this(new DidService());
    }

    public DidRegistryService(DidService didService) {
        this.didService = didService;
    }

    public DidRegistration registerDidKey(String did, String ownerId, String publicKeyMultibase) {
        DidDocument document = didService.createDidKeyDocument(did, publicKeyMultibase);
        return storeRegistration(did, ownerId, document);
    }

    public DidRegistration registerDidWeb(String did, String ownerId, Map<String, Object> publicKeyJwk) {
        DidDocument document = didService.createDidWebDocument(did, publicKeyJwk);
        return storeRegistration(did, ownerId, document);
    }

    public DidRegistration resolve(String did) {
        return registrations.get(did);
    }

    public List<DidRegistration> listAll() {
        return new ArrayList<>(registrations.values());
    }

    public boolean deactivate(String did) {
        return registrations.remove(did) != null;
    }

    private DidRegistration storeRegistration(String did, String ownerId, DidDocument document) {
        DidRegistration registration = new DidRegistration();
        registration.setDid(did);
        registration.setOwnerId(ownerId);
        registration.setRegisteredAt(Instant.now());
        registration.setDocument(document);

        registrations.put(did, registration);
        return registration;
    }
}

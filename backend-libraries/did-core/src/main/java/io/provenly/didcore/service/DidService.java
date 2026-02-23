package io.provenly.didcore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.provenly.didcore.model.DidDocument;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Core DID operations used across issuer, verifier and wallet flows.
 */
public class DidService {

    private static final String W3C_DID_CONTEXT = "https://www.w3.org/ns/did/v1";
    private static final Pattern DID_PATTERN = Pattern.compile("^did:[a-z0-9]+:[A-Za-z0-9._:%-]+$");

    private final ObjectMapper objectMapper;

    public DidService() {
        this(new ObjectMapper());
    }

    public DidService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isValidDid(String did) {
        return did != null && DID_PATTERN.matcher(did).matches();
    }

    public String getDidMethod(String did) {
        if (!isValidDid(did)) {
            throw new IllegalArgumentException("Invalid DID format: " + did);
        }

        String[] parts = did.split(":", 4);
        return parts[1].toLowerCase(Locale.ROOT);
    }

    public DidDocument createDidKeyDocument(String did, String publicKeyMultibase) {
        validateDidMethod(did, "key");

        String keyRef = did + "#keys-1";

        DidDocument document = new DidDocument();
        document.setId(did);
        document.setContext(List.of(W3C_DID_CONTEXT));
        document.setVerificationMethod(List.of(Map.of(
                "id", keyRef,
                "type", "Multikey",
                "controller", did,
                "publicKeyMultibase", publicKeyMultibase
        )));
        document.setAuthentication(List.of(keyRef));
        document.setAssertionMethod(List.of(keyRef));
        return document;
    }

    public DidDocument createDidWebDocument(String did, Map<String, Object> publicKeyJwk) {
        validateDidMethod(did, "web");

        String keyRef = did + "#owner";

        DidDocument document = new DidDocument();
        document.setId(did);
        document.setContext(List.of(W3C_DID_CONTEXT));
        document.setController(List.of(did));
        document.setVerificationMethod(List.of(Map.of(
                "id", keyRef,
                "type", "JsonWebKey2020",
                "controller", did,
                "publicKeyJwk", publicKeyJwk
        )));
        document.setAuthentication(List.of(keyRef));
        document.setAssertionMethod(List.of(keyRef));
        return document;
    }

    public String toJson(DidDocument didDocument) {
        try {
            return objectMapper.writeValueAsString(didDocument);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to serialize DID document", exception);
        }
    }

    public DidDocument fromJson(String json) {
        try {
            return objectMapper.readValue(json, DidDocument.class);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to deserialize DID document", exception);
        }
    }

    private void validateDidMethod(String did, String expectedMethod) {
        String actualMethod = getDidMethod(did);
        if (!expectedMethod.equals(actualMethod)) {
            throw new IllegalArgumentException(
                    "DID method mismatch: expected '" + expectedMethod + "' but got '" + actualMethod + "'"
            );
        }
    }
}

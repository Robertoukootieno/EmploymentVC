package io.provenly.didcore.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal DID Document representation for resolver and issuer flows.
 */
public class DidDocument {

    private String id;
    private List<String> context = new ArrayList<>();
    private List<String> controller = new ArrayList<>();
    private List<Map<String, Object>> verificationMethod = new ArrayList<>();
    private List<String> authentication = new ArrayList<>();
    private List<String> assertionMethod = new ArrayList<>();
    private Map<String, Object> metadata = new LinkedHashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getContext() {
        return context;
    }

    public void setContext(List<String> context) {
        this.context = context;
    }

    public List<String> getController() {
        return controller;
    }

    public void setController(List<String> controller) {
        this.controller = controller;
    }

    public List<Map<String, Object>> getVerificationMethod() {
        return verificationMethod;
    }

    public void setVerificationMethod(List<Map<String, Object>> verificationMethod) {
        this.verificationMethod = verificationMethod;
    }

    public List<String> getAuthentication() {
        return authentication;
    }

    public void setAuthentication(List<String> authentication) {
        this.authentication = authentication;
    }

    public List<String> getAssertionMethod() {
        return assertionMethod;
    }

    public void setAssertionMethod(List<String> assertionMethod) {
        this.assertionMethod = assertionMethod;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

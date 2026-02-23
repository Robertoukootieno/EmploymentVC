package io.provenly.sdjwtslib.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SD-JWT payload representation containing mandatory and selectively disclosable claims.
 */
public class SdJwtPayload {

    private Map<String, Object> mandatoryClaims = new LinkedHashMap<>();
    private Map<String, Object> disclosableClaims = new LinkedHashMap<>();

    public Map<String, Object> getMandatoryClaims() {
        return mandatoryClaims;
    }

    public void setMandatoryClaims(Map<String, Object> mandatoryClaims) {
        this.mandatoryClaims = mandatoryClaims;
    }

    public Map<String, Object> getDisclosableClaims() {
        return disclosableClaims;
    }

    public void setDisclosableClaims(Map<String, Object> disclosableClaims) {
        this.disclosableClaims = disclosableClaims;
    }
}

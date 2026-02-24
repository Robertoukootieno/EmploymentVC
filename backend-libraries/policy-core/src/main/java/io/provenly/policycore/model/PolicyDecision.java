package io.provenly.policycore.model;

/**
 * Policy evaluation decision result.
 */
public class PolicyDecision {

    private final boolean allowed;
    private final String reason;

    public PolicyDecision(boolean allowed, String reason) {
        this.allowed = allowed;
        this.reason = reason;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getReason() {
        return reason;
    }
}

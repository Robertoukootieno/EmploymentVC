package io.provenly.policycore.service;

import io.provenly.policycore.model.PolicyDecision;

import java.util.Map;

/**
 * Basic policy evaluator for attribute-based checks.
 */
public class PolicyEvaluationService {

    public PolicyDecision evaluateEqualsPolicy(String attributeName, String expectedValue, Map<String, Object> subject) {
        Object value = subject.get(attributeName);
        boolean allowed = value != null && expectedValue.equals(String.valueOf(value));

        if (allowed) {
            return new PolicyDecision(true, "Policy matched");
        }

        return new PolicyDecision(false, "Attribute mismatch for: " + attributeName);
    }

    public PolicyDecision evaluatePresencePolicy(String attributeName, Map<String, Object> subject) {
        boolean allowed = subject.containsKey(attributeName) && subject.get(attributeName) != null;
        return allowed
                ? new PolicyDecision(true, "Attribute present")
                : new PolicyDecision(false, "Missing required attribute: " + attributeName);
    }
}

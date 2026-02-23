package io.provenly.apigateway.security.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Input validation utility for preventing injection attacks
 * 
 * Checks for:
 * - SQL injection patterns
 * - XSS patterns
 * - Path traversal
 * - Command injection
 * - XXE patterns
 */
@Slf4j
@Component
public class InputValidator {

    // Regex patterns for common injection attempts
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|exec|execute|script|javascript|onerror|onload)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|javascript:|onerror|onload|<iframe|<svg)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(\\.\\./|\\.\\\\|%2e%2e)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "([;&|`$()\\n\\r])",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern XXE_PATTERN = Pattern.compile(
        "(?i)(<!DOCTYPE|<!ENTITY|SYSTEM|PUBLIC)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Validates input against common injection patterns
     * @param input User input to validate
     * @param fieldName Name of the field being validated (for logging)
     * @return true if input is valid, false if suspicious patterns detected
     */
    public boolean validateInput(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            return true; // Empty is valid, validation is done elsewhere
        }

        if (isSuspiciousSQLInjection(input)) {
            log.warn("Potential SQL injection detected in field '{}': {}", fieldName, input);
            return false;
        }

        if (isSuspiciousXSS(input)) {
            log.warn("Potential XSS detected in field '{}': {}", fieldName, input);
            return false;
        }

        if (isSuspiciousPathTraversal(input)) {
            log.warn("Potential path traversal detected in field '{}': {}", fieldName, input);
            return false;
        }

        if (isSuspiciousCommandInjection(input)) {
            log.warn("Potential command injection detected in field '{}': {}", fieldName, input);
            return false;
        }

        if (isSuspiciousXXE(input)) {
            log.warn("Potential XXE detected in field '{}': {}", fieldName, input);
            return false;
        }

        return true;
    }

    private boolean isSuspiciousSQLInjection(String input) {
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    private boolean isSuspiciousXSS(String input) {
        return XSS_PATTERN.matcher(input).find();
    }

    private boolean isSuspiciousPathTraversal(String input) {
        return PATH_TRAVERSAL_PATTERN.matcher(input).find();
    }

    private boolean isSuspiciousCommandInjection(String input) {
        return COMMAND_INJECTION_PATTERN.matcher(input).find();
    }

    private boolean isSuspiciousXXE(String input) {
        return XXE_PATTERN.matcher(input).find();
    }

    /**
     * Sanitizes input by removing potentially dangerous characters
     * @param input Input to sanitize
     * @return Sanitized input
     */
    public String sanitize(String input) {
        if (input == null) {
            return null;
        }

        return input
            .replaceAll("[<>\"'`]", "") // Remove HTML special chars
            .replaceAll("(?i)(javascript:|onerror=|onload=)", "") // Remove event handlers
            .trim();
    }

    /**
     * Encodes input for safe HTML output
     * @param input Input to encode
     * @return HTML-safe string
     */
    public String htmlEncode(String input) {
        if (input == null) {
            return null;
        }

        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }
}

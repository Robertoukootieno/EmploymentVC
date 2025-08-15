package io.provenly.commons.exception;

import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base exception class for all Provenly platform exceptions.
 * Provides structured error information with correlation IDs and context.
 */
@Getter
public class ProvenlyException extends RuntimeException {

    /**
     * Unique identifier for this error occurrence.
     */
    private final String errorId;

    /**
     * Error code for categorizing the error.
     */
    private final String errorCode;

    /**
     * When the error occurred.
     */
    private final Instant timestamp;

    /**
     * Additional context information about the error.
     */
    private final Map<String, Object> context;

    /**
     * HTTP status code associated with this error.
     */
    private final int httpStatus;

    public ProvenlyException(String message) {
        this(message, "GENERAL_ERROR", 500, null, null);
    }

    public ProvenlyException(String message, Throwable cause) {
        this(message, "GENERAL_ERROR", 500, null, cause);
    }

    public ProvenlyException(String message, String errorCode, int httpStatus) {
        this(message, errorCode, httpStatus, null, null);
    }

    public ProvenlyException(String message, String errorCode, int httpStatus, Map<String, Object> context) {
        this(message, errorCode, httpStatus, context, null);
    }

    public ProvenlyException(String message, String errorCode, int httpStatus, Map<String, Object> context, Throwable cause) {
        super(message, cause);
        this.errorId = UUID.randomUUID().toString();
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.timestamp = Instant.now();
        this.context = context;
    }

    /**
     * Get a formatted error message including error ID and timestamp.
     */
    public String getFormattedMessage() {
        return String.format("[%s] %s (Error ID: %s, Time: %s)", 
            errorCode, getMessage(), errorId, timestamp);
    }

    /**
     * Check if this exception has additional context information.
     */
    public boolean hasContext() {
        return context != null && !context.isEmpty();
    }
}

/**
 * Exception for authentication-related errors.
 */
class AuthenticationException extends ProvenlyException {
    public AuthenticationException(String message) {
        super(message, "AUTH_ERROR", 401);
    }

    public AuthenticationException(String message, Map<String, Object> context) {
        super(message, "AUTH_ERROR", 401, context);
    }
}

/**
 * Exception for authorization-related errors.
 */
class AuthorizationException extends ProvenlyException {
    public AuthorizationException(String message) {
        super(message, "AUTHZ_ERROR", 403);
    }

    public AuthorizationException(String message, Map<String, Object> context) {
        super(message, "AUTHZ_ERROR", 403, context);
    }
}

/**
 * Exception for validation errors.
 */
class ValidationException extends ProvenlyException {
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", 400);
    }

    public ValidationException(String message, Map<String, Object> context) {
        super(message, "VALIDATION_ERROR", 400, context);
    }
}

/**
 * Exception for resource not found errors.
 */
class ResourceNotFoundException extends ProvenlyException {
    public ResourceNotFoundException(String message) {
        super(message, "NOT_FOUND", 404);
    }

    public ResourceNotFoundException(String message, Map<String, Object> context) {
        super(message, "NOT_FOUND", 404, context);
    }
}

/**
 * Exception for credential-related errors.
 */
class CredentialException extends ProvenlyException {
    public CredentialException(String message) {
        super(message, "CREDENTIAL_ERROR", 422);
    }

    public CredentialException(String message, Map<String, Object> context) {
        super(message, "CREDENTIAL_ERROR", 422, context);
    }

    public CredentialException(String message, Throwable cause) {
        super(message, "CREDENTIAL_ERROR", 422, null, cause);
    }
}

/**
 * Exception for wallet-related errors.
 */
class WalletException extends ProvenlyException {
    public WalletException(String message) {
        super(message, "WALLET_ERROR", 422);
    }

    public WalletException(String message, Map<String, Object> context) {
        super(message, "WALLET_ERROR", 422, context);
    }

    public WalletException(String message, Throwable cause) {
        super(message, "WALLET_ERROR", 422, null, cause);
    }
}

/**
 * Exception for cryptographic operation errors.
 */
class CryptographicException extends ProvenlyException {
    public CryptographicException(String message) {
        super(message, "CRYPTO_ERROR", 500);
    }

    public CryptographicException(String message, Throwable cause) {
        super(message, "CRYPTO_ERROR", 500, null, cause);
    }
}

/**
 * Exception for DID-related errors.
 */
class DidException extends ProvenlyException {
    public DidException(String message) {
        super(message, "DID_ERROR", 422);
    }

    public DidException(String message, Map<String, Object> context) {
        super(message, "DID_ERROR", 422, context);
    }

    public DidException(String message, Throwable cause) {
        super(message, "DID_ERROR", 422, null, cause);
    }
}

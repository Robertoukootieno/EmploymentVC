package io.provenly.auth.config;

import io.provenly.auth.exception.AccountLockedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for authentication errors
 */
@Slf4j
@RestControllerAdvice
public class AuthenticationExceptionHandler {

    /**
     * Handle AccountLockedException
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(
            AccountLockedException ex,
            WebRequest request) {

        log.warn("Account locked error: {}", ex.getMessage());

        return ResponseEntity
            .status(HttpStatus.LOCKED)
            .body(ErrorResponse.builder()
                .status(429)
                .message(ex.getMessage())
                .error("Account Locked")
                .timestamp(System.currentTimeMillis())
                .retryAfterSeconds(ex.getSecondsUntilUnlock())
                .build());
    }

    /**
     * Handle BadCredentialsException
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            WebRequest request) {

        log.warn("Bad credentials error: {}", ex.getMessage());

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse.builder()
                .status(401)
                .message(ex.getMessage())
                .error("Bad Credentials")
                .timestamp(System.currentTimeMillis())
                .build());
    }

    /**
     * Error response DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private int status;
        private String message;
        private String error;
        private long timestamp;
        private Long retryAfterSeconds;
    }
}

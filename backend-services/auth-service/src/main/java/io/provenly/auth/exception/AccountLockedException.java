package io.provenly.auth.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Exception thrown when account is locked due to too many failed login attempts
 */
public class AccountLockedException extends AuthenticationException {
    private final long secondsUntilUnlock;

    public AccountLockedException(String message, long secondsUntilUnlock) {
        super(message);
        this.secondsUntilUnlock = secondsUntilUnlock;
    }

    public long getSecondsUntilUnlock() {
        return secondsUntilUnlock;
    }
}

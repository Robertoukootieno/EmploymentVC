package io.provenly.auth.security;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Failed Login Throttling Component
 * Prevents brute force attacks by tracking failed login attempts per user/IP
 */
@Slf4j
@Component
public class FailedLoginThrottleManager {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;
    private static final long ATTEMPT_RESET_MINUTES = 30;

    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public FailedLoginThrottleManager() {
        // Cleanup expired entries every 5 minutes
        executorService.scheduleAtFixedRate(this::cleanupExpiredEntries, 5, 5, TimeUnit.MINUTES);
    }

    /**
     * Check if account is locked due to failed attempts
     */
    public boolean isAccountLocked(String username, String ipAddress) {
        final String key = generateKey(username, ipAddress);
        final LoginAttempt attempt = loginAttempts.get(key);

        if (attempt == null) {
            return false;
        }

        if (attempt.isLocked()) {
            log.warn("Account locked for user: {} from IP: {}", username, ipAddress);
            return true;
        }

        return false;
    }

    /**
     * Record a failed login attempt
     * Returns true if account should be locked
     */
    public boolean recordFailedAttempt(String username, String ipAddress) {
        final String key = generateKey(username, ipAddress);
        final long now = System.currentTimeMillis();

        final LoginAttempt attempt = loginAttempts.computeIfAbsent(key, k -> {
            log.info("New login attempt tracked for user: {} from IP: {}", username, ipAddress);
            return new LoginAttempt(username, ipAddress);
        });

        // Reset attempts if outside the reset window
        if (now - attempt.getFirstAttemptTime() > ATTEMPT_RESET_MINUTES * 60 * 1000) {
            attempt.reset(now);
            log.info("Login attempts reset for user: {} from IP: {} (reset window expired)", username, ipAddress);
        }

        attempt.recordFailedAttempt(now);
        log.warn("Failed login attempt #{} for user: {} from IP: {} in last {} minutes",
            attempt.getFailedAttempts(), username, ipAddress, ATTEMPT_RESET_MINUTES);

        // Lock if max attempts exceeded
        if (attempt.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            attempt.lock(now);
            log.error("Account locked after {} failed attempts for user: {} from IP: {}",
                MAX_FAILED_ATTEMPTS, username, ipAddress);
            return true;
        }

        return false;
    }

    /**
     * Record a successful login
     * Resets the failed attempt counter
     */
    public void recordSuccessfulLogin(String username, String ipAddress) {
        final String key = generateKey(username, ipAddress);
        loginAttempts.remove(key);
        log.info("Successful login for user: {} from IP: {}", username, ipAddress);
    }

    /**
     * Get remaining time until account unlock (in seconds)
     * Returns 0 if account is not locked
     */
    public long getTimeUntilUnlock(String username, String ipAddress) {
        final String key = generateKey(username, ipAddress);
        final LoginAttempt attempt = loginAttempts.get(key);

        if (attempt == null || !attempt.isLocked()) {
            return 0;
        }

        final long remainingMillis = (LOCKOUT_DURATION_MINUTES * 60 * 1000) -
            (System.currentTimeMillis() - attempt.getLockedTime());

        return Math.max(0, remainingMillis / 1000);
    }

    /**
     * Get number of failed attempts remaining
     */
    public int getRemainingAttempts(String username, String ipAddress) {
        final String key = generateKey(username, ipAddress);
        final LoginAttempt attempt = loginAttempts.get(key);

        if (attempt == null) {
            return MAX_FAILED_ATTEMPTS;
        }

        return Math.max(0, MAX_FAILED_ATTEMPTS - attempt.getFailedAttempts());
    }

    /**
     * Manually unlock an account (admin function)
     */
    public void unlockAccount(String username, String ipAddress) {
        final String key = generateKey(username, ipAddress);
        loginAttempts.remove(key);
        log.warn("Account manually unlocked for user: {} from IP: {}", username, ipAddress);
    }

    /**
     * Get statistics for monitoring
     */
    public LoginThrottleStats getStats() {
        final int totalTracked = loginAttempts.size();
        final int locked = (int) loginAttempts.values().stream()
            .filter(LoginAttempt::isLocked)
            .count();

        return LoginThrottleStats.builder()
            .totalTrackedAccounts(totalTracked)
            .lockedAccounts(locked)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * Cleanup expired entries (accounts that haven't had activity)
     */
    private void cleanupExpiredEntries() {
        final long currentTime = System.currentTimeMillis();
        final long expirationTime = ATTEMPT_RESET_MINUTES * 60 * 1000;

        loginAttempts.entrySet().removeIf(entry -> {
            final LoginAttempt attempt = entry.getValue();
            // Remove if no activity for the reset period and not locked
            if (!attempt.isLocked() &&
                currentTime - attempt.getLastAttemptTime() > expirationTime) {
                log.debug("Cleaned up expired entry for: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    private String generateKey(String username, String ipAddress) {
        return username + ":" + ipAddress;
    }

    /**
     * Inner class to track login attempts
     */
    private static class LoginAttempt {
        private int failedAttempts;
        private long firstAttemptTime;
        private long lastAttemptTime;
        private long lockedTime;
        private boolean locked;

        LoginAttempt(String username, String ipAddress) {
            this.firstAttemptTime = System.currentTimeMillis();
            this.failedAttempts = 0;
            this.locked = false;
        }

        void recordFailedAttempt(long timestamp) {
            this.failedAttempts++;
            this.lastAttemptTime = timestamp;
        }

        void lock(long timestamp) {
            this.locked = true;
            this.lockedTime = timestamp;
        }

        void reset(long timestamp) {
            this.failedAttempts = 0;
            this.firstAttemptTime = timestamp;
            this.lastAttemptTime = timestamp;
            this.locked = false;
        }

        boolean isLocked() {
            if (!locked) {
                return false;
            }
            final long lockDurationMillis = LOCKOUT_DURATION_MINUTES * 60 * 1000;
            if (System.currentTimeMillis() - lockedTime > lockDurationMillis) {
                this.locked = false;
                return false;
            }
            return true;
        }

        int getFailedAttempts() {
            return failedAttempts;
        }

        long getFirstAttemptTime() {
            return firstAttemptTime;
        }

        long getLastAttemptTime() {
            return lastAttemptTime;
        }

        long getLockedTime() {
            return lockedTime;
        }
    }
}

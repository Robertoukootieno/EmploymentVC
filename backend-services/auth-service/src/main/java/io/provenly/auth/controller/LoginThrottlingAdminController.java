package io.provenly.auth.controller;

import io.provenly.auth.security.FailedLoginThrottleManager;
import io.provenly.auth.security.LoginThrottleStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin endpoints for managing login throttling
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/security")
@PreAuthorize("hasRole('ADMIN')")
public class LoginThrottlingAdminController {

    @Autowired
    private FailedLoginThrottleManager throttleManager;

    /**
     * Get throttling statistics
     */
    @GetMapping("/throttle/stats")
    public ResponseEntity<LoginThrottleStats> getThrottlingStats() {
        log.info("Retrieved throttling statistics");
        return ResponseEntity.ok(throttleManager.getStats());
    }

    /**
     * Unlock a user account
     */
    @PostMapping("/throttle/unlock/{username}")
    public ResponseEntity<String> unlockAccount(
            @PathVariable String username,
            @RequestParam(required = false) String ipAddress) {
        
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = "0.0.0.0";  // Will match any IP for this username
        }

        throttleManager.unlockAccount(username, ipAddress);
        log.warn("Admin unlocked account for user: {} IP: {}", username, ipAddress);
        
        return ResponseEntity.ok("Account unlocked successfully");
    }

    /**
     * Get lockout status for a user
     */
    @GetMapping("/throttle/status/{username}")
    public ResponseEntity<ThrottleStatusResponse> getThrottleStatus(
            @PathVariable String username,
            @RequestParam String ipAddress) {

        final boolean isLocked = throttleManager.isAccountLocked(username, ipAddress);
        final long secondsUntilUnlock = throttleManager.getTimeUntilUnlock(username, ipAddress);
        final int remainingAttempts = throttleManager.getRemainingAttempts(username, ipAddress);

        return ResponseEntity.ok(ThrottleStatusResponse.builder()
            .username(username)
            .ipAddress(ipAddress)
            .isLocked(isLocked)
            .secondsUntilUnlock(secondsUntilUnlock)
            .remainingAttempts(remainingAttempts)
            .build());
    }

    /**
     * Response DTO for throttle status
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ThrottleStatusResponse {
        private String username;
        private String ipAddress;
        private boolean isLocked;
        private long secondsUntilUnlock;
        private int remainingAttempts;
    }
}

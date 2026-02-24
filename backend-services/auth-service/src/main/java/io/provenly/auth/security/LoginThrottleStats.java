package io.provenly.auth.security;

import lombok.*;

/**
 * DTO for login throttle statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginThrottleStats {
    private int totalTrackedAccounts;
    private int lockedAccounts;
    private long timestamp;
}

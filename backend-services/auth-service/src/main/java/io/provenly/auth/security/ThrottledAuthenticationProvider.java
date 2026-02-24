package io.provenly.auth.security;

import io.provenly.auth.exception.AccountLockedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Custom Authentication Provider with Failed Login Throttling
 * Extends DaoAuthenticationProvider to add throttling capability
 */
@Slf4j
@Component
public class ThrottledAuthenticationProvider extends DaoAuthenticationProvider {

    @Autowired
    private FailedLoginThrottleManager throttleManager;

    @Autowired
    private ClientIpResolver clientIpResolver;

    public ThrottledAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        setUserDetailsService(userDetailsService);
        setPasswordEncoder(passwordEncoder);
        setHideUserNotFoundExceptions(false);  // Consistent error messages
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        final String username = authentication.getName();
        final String clientIp = clientIpResolver.getClientIp();

        log.debug("Authentication attempt for user: {} from IP: {}", username, clientIp);

        // Check if account is locked
        if (throttleManager.isAccountLocked(username, clientIp)) {
            final long secondsUntilUnlock = throttleManager.getTimeUntilUnlock(username, clientIp);
            log.warn("Blocked login attempt for locked account: {} from IP: {} ({}s remaining)",
                username, clientIp, secondsUntilUnlock);
            throw new AccountLockedException(
                "Account is temporarily locked due to too many failed login attempts. " +
                "Please try again in " + secondsUntilUnlock + " seconds.",
                secondsUntilUnlock);
        }

        try {
            // Attempt authentication
            final Authentication result = super.authenticate(authentication);
            
            // Success - reset failed attempts
            throttleManager.recordSuccessfulLogin(username, clientIp);
            log.info("Successful authentication for user: {} from IP: {}", username, clientIp);
            
            return result;
        } catch (BadCredentialsException e) {
            // Failed attempt - record it
            final boolean isLocked = throttleManager.recordFailedAttempt(username, clientIp);
            
            if (isLocked) {
                log.warn("Account locked for user: {} from IP: {} after max failed attempts",
                    username, clientIp);
                throw new AccountLockedException(
                    "Account is temporarily locked due to too many failed login attempts. " +
                    "Please try again in " + (15 * 60) + " seconds.",
                    15 * 60);
            }

            final int remainingAttempts = throttleManager.getRemainingAttempts(username, clientIp);
            log.warn("Failed authentication for user: {} from IP: {} ({} attempts remaining)",
                username, clientIp, remainingAttempts);
            
            // Custom error message with remaining attempts
            throw new BadCredentialsException(
                "Invalid credentials. " + remainingAttempts + " attempts remaining before account lockout.",
                e);
        } catch (AuthenticationException e) {
            // Other authentication errors (user not found, etc.)
            log.warn("Authentication error for user: {} from IP: {}: {}",
                username, clientIp, e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.isAssignableFrom(UsernamePasswordAuthenticationToken.class);
    }
}

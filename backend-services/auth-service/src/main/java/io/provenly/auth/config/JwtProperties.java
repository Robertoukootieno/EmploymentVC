package io.provenly.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for JWT tokens.
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Secret key for signing JWT tokens.
     * In production, this should be loaded from environment variables or secrets manager.
     */
    private String secret = "provenly-jwt-secret-key-change-this-in-production-minimum-256-bits";

    /**
     * JWT token expiration time in hours.
     */
    private int expirationHours = 24;

    /**
     * Refresh token expiration time in days.
     */
    private int refreshExpirationDays = 30;

    /**
     * JWT issuer.
     */
    private String issuer = "provenly-auth-service";

    /**
     * JWT audience.
     */
    private String audience = "provenly-platform";
}


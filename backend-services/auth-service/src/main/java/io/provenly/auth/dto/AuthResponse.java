package io.provenly.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for authentication operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * JWT access token.
     */
    private String accessToken;

    /**
     * Refresh token for obtaining new access tokens.
     */
    private String refreshToken;

    /**
     * Token expiration time in seconds.
     */
    private long expiresIn;

    /**
     * Token type (usually "Bearer").
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * User information.
     */
    private UserDto user;
}


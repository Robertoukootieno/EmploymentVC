package io.provenly.auth.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for authentication operations.
 */
@Data
@NoArgsConstructor
@SuppressFBWarnings(value = {"EI", "EI2"}, justification = "DTO uses defensive copies for mutable fields.")
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
    private String tokenType = "Bearer";

    /**
     * User information.
     */
    private UserDto user;

    @Builder
    public AuthResponse(String accessToken,
                        String refreshToken,
                        long expiresIn,
                        String tokenType,
                        UserDto user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.tokenType = tokenType == null ? "Bearer" : tokenType;
        this.user = UserDto.copyOf(user);
    }

    public UserDto getUser() {
        return UserDto.copyOf(user);
    }

    public void setUser(UserDto user) {
        this.user = UserDto.copyOf(user);
    }
}


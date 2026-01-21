package io.provenly.auth.service;

import io.provenly.auth.dto.*;
import io.provenly.auth.model.RefreshToken;
import io.provenly.auth.model.User;
import io.provenly.auth.repository.RefreshTokenRepository;
import io.provenly.auth.repository.UserRepository;
import io.provenly.commons.exception.ProvenlyException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

/**
 * Main authentication service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final Web3AuthService web3AuthService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Traditional email/password login.
     */
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ProvenlyException.AuthenticationException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ProvenlyException.AuthenticationException("Invalid email or password");
        }

        // Check if user is enabled
        if (!user.isEnabled()) {
            throw new ProvenlyException.AuthenticationException("Account is disabled");
        }

        // Update last login
        user.updateLastLogin();
        userRepository.save(user);

        // Generate tokens
        return generateAuthResponse(user, httpRequest);
    }

    /**
     * Web3 wallet authentication.
     */
    @Transactional
    public AuthResponse web3Login(Web3VerifyRequest request, HttpServletRequest httpRequest) {
        // Verify signature
        boolean isValid = web3AuthService.verifySignature(
                request.getWalletAddress(),
                request.getSignature(),
                request.getMessage()
        );

        if (!isValid) {
            throw new ProvenlyException.AuthenticationException("Invalid wallet signature");
        }

        // Find or create user
        User user = userRepository.findByWalletAddress(request.getWalletAddress())
                .orElseGet(() -> createWeb3User(request.getWalletAddress()));

        // Check if user is enabled
        if (!user.isEnabled()) {
            throw new ProvenlyException.AuthenticationException("Account is disabled");
        }

        // Update last login
        user.updateLastLogin();
        userRepository.save(user);

        // Generate tokens
        return generateAuthResponse(user, httpRequest);
    }

    /**
     * Refresh access token.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        // Find refresh token
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new ProvenlyException.AuthenticationException("Invalid refresh token"));

        // Validate token
        if (!refreshToken.isValid()) {
            throw new ProvenlyException.AuthenticationException("Refresh token is expired or revoked");
        }

        // Find user
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ProvenlyException.ResourceNotFoundException("User not found"));

        // Generate new tokens
        return generateAuthResponse(user, httpRequest);
    }

    /**
     * Logout user (revoke refresh tokens).
     */
    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("User logged out: {}", userId);
    }

    /**
     * Get current user.
     */
    public UserDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ProvenlyException.ResourceNotFoundException("User not found"));
        
        return mapToUserDto(user);
    }

    /**
     * Generate authentication response with tokens.
     */
    private AuthResponse generateAuthResponse(User user, HttpServletRequest httpRequest) {
        // Generate access token
        String accessToken = jwtService.generateAccessToken(user);

        // Generate refresh token
        String refreshTokenValue = jwtService.generateRefreshToken(user.getId());

        // Save refresh token to database
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .userId(user.getId())
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .ipAddress(getClientIp(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .tokenType("Bearer")
                .user(mapToUserDto(user))
                .build();
    }

    /**
     * Create new user from Web3 wallet.
     */
    private User createWeb3User(String walletAddress) {
        User user = User.builder()
                .walletAddress(walletAddress)
                .name("User " + walletAddress.substring(0, 8))
                .roles(Set.of("ROLE_USER"))
                .enabled(true)
                .build();

        return userRepository.save(user);
    }

    /**
     * Map User entity to UserDto.
     */
    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .roles(user.getRoles())
                .walletAddress(user.getWalletAddress())
                .did(user.getDid())
                .emailVerified(user.isEmailVerified())
                .enabled(user.isEnabled())
                .build();
    }

    /**
     * Get client IP address from request.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}


package io.provenly.auth.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.provenly.auth.dto.*;
import io.provenly.auth.model.User;
import io.provenly.auth.repository.UserRepository;
import io.provenly.auth.security.JwtTokenService;
import io.provenly.auth.security.RefreshTokenStore;
import io.provenly.commons.exception.ProvenlyException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Main authentication service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI2", justification = "Injected dependencies are managed by Spring and not exposed.")
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenStore refreshTokenStore;
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

        // Generate tokens using JwtTokenService and RefreshTokenStore
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("name", user.getName());
        claims.put("roles", user.getRoles());
        if (user.getWalletAddress() != null) claims.put("walletAddress", user.getWalletAddress());
        if (user.getDid() != null) claims.put("did", user.getDid());

        String username = user.getEmail();
        String tokenId = UUID.randomUUID().toString();
        refreshTokenStore.store(tokenId, username);
        String accessToken = jwtTokenService.generateAccessToken(username, claims);
        String refreshToken = jwtTokenService.generateRefreshToken(username, tokenId);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(15 * 60) // match JwtTokenService config
                .tokenType("Bearer")
                .user(mapToUserDto(user))
                .build();
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

        // Generate tokens using JwtTokenService and RefreshTokenStore
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("name", user.getName());
        claims.put("roles", user.getRoles());
        if (user.getWalletAddress() != null) claims.put("walletAddress", user.getWalletAddress());
        if (user.getDid() != null) claims.put("did", user.getDid());

        String username = user.getEmail();
        String tokenId = UUID.randomUUID().toString();
        refreshTokenStore.store(tokenId, username);
        String accessToken = jwtTokenService.generateAccessToken(username, claims);
        String refreshToken = jwtTokenService.generateRefreshToken(username, tokenId);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(15 * 60)
                .tokenType("Bearer")
                .user(mapToUserDto(user))
                .build();
    }

    /**
     * Refresh access token.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String refreshToken = request.getRefreshToken();
        String username;
        String tokenId;
        try {
            username = jwtTokenService.getUsername(refreshToken);
            tokenId = jwtTokenService.getTokenId(refreshToken);
        } catch (Exception e) {
            throw new ProvenlyException.AuthenticationException("Invalid refresh token");
        }

        // Validate refresh token
        if (!refreshTokenStore.isValid(tokenId, username) || jwtTokenService.isTokenExpired(refreshToken)) {
            throw new ProvenlyException.AuthenticationException("Invalid or expired refresh token");
        }

        // Rotate: revoke old, issue new
        refreshTokenStore.revoke(tokenId);
        String newTokenId = UUID.randomUUID().toString();
        refreshTokenStore.store(newTokenId, username);

        // Find user
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ProvenlyException.ResourceNotFoundException("User not found"));

        // Issue new tokens
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("name", user.getName());
        claims.put("roles", user.getRoles());
        if (user.getWalletAddress() != null) claims.put("walletAddress", user.getWalletAddress());
        if (user.getDid() != null) claims.put("did", user.getDid());

        String accessToken = jwtTokenService.generateAccessToken(username, claims);
        String newRefreshToken = jwtTokenService.generateRefreshToken(username, newTokenId);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(15 * 60)
                .tokenType("Bearer")
                .user(mapToUserDto(user))
                .build();
    }

    /**
     * Logout user (revoke refresh tokens).
     */
    @Transactional
    public void logout(UUID userId) {
        // Invalidate all refresh tokens for this user (in-memory store: not implemented per-user, so this is a no-op)
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

        // generateAuthResponse is now inlined in login/web3Login/refreshToken

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

}


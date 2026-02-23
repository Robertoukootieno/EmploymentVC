package io.provenly.auth.controller;

import io.provenly.auth.security.JwtTokenService;
import io.provenly.auth.security.RefreshTokenStore;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthTokenController {
    @Autowired
    private JwtTokenService jwtTokenService;
    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            String username = jwtTokenService.getUsername(refreshToken);
            String tokenId = jwtTokenService.getTokenId(refreshToken);

            // Validate refresh token
            if (!refreshTokenStore.isValid(tokenId, username) || jwtTokenService.isTokenExpired(refreshToken)) {
                return ResponseEntity.status(401).body("Invalid or expired refresh token");
            }

            // Rotate: revoke old, issue new
            refreshTokenStore.revoke(tokenId);
            String newTokenId = UUID.randomUUID().toString();
            refreshTokenStore.store(newTokenId, username);

            // Issue new tokens
            Map<String, Object> claims = new HashMap<>();
            String newAccessToken = jwtTokenService.generateAccessToken(username, claims);
            String newRefreshToken = jwtTokenService.generateRefreshToken(username, newTokenId);

            Map<String, String> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("refreshToken", newRefreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid refresh token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            String tokenId = jwtTokenService.getTokenId(refreshToken);
            refreshTokenStore.revoke(tokenId);
            return ResponseEntity.ok("Logged out");
        } catch (Exception e) {
            return ResponseEntity.ok("Logged out");
        }
    }

    @Data
    public static class RefreshRequest {
        private String refreshToken;
    }
}

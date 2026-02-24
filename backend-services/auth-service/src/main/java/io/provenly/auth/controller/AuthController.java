package io.provenly.auth.controller;

import io.provenly.auth.dto.*;
import io.provenly.auth.service.AuthService;
import io.provenly.auth.service.JwtService;
import io.provenly.commons.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for traditional authentication endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Traditional authentication endpoints")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    /**
     * Traditional email/password login.
     */
    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Login attempt for email: {}", request.getEmail());
        
        AuthResponse response = authService.login(request, httpRequest);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    /**
     * Refresh access token.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Token refresh request");
        
        AuthResponse response = authService.refreshToken(request, httpRequest);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    /**
     * Logout user.
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        
        // Extract token from Authorization header
        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.getUserIdFromToken(token);
        
        log.info("Logout request for user: {}", userId);
        
        authService.logout(userId);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }

    /**
     * Get current user information.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user information")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        
        // Extract token from Authorization header
        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.getUserIdFromToken(token);
        
        log.info("Get current user request for: {}", userId);
        
        UserDto user = authService.getCurrentUser(userId);
        
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}


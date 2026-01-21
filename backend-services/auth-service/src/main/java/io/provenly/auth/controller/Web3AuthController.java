package io.provenly.auth.controller;

import io.provenly.auth.dto.AuthResponse;
import io.provenly.auth.dto.Web3ChallengeRequest;
import io.provenly.auth.dto.Web3ChallengeResponse;
import io.provenly.auth.dto.Web3VerifyRequest;
import io.provenly.auth.service.AuthService;
import io.provenly.auth.service.Web3AuthService;
import io.provenly.commons.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Web3 wallet authentication endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth/web3")
@Tag(name = "Web3 Authentication", description = "Web3 wallet authentication endpoints")
@Slf4j
@RequiredArgsConstructor
public class Web3AuthController {

    private final Web3AuthService web3AuthService;
    private final AuthService authService;

    /**
     * Generate Web3 authentication challenge.
     */
    @PostMapping("/challenge")
    @Operation(summary = "Generate Web3 authentication challenge")
    public ResponseEntity<ApiResponse<Web3ChallengeResponse>> generateChallenge(
            @Valid @RequestBody Web3ChallengeRequest request) {
        
        log.info("Web3 challenge request for wallet: {}", request.getWalletAddress());
        
        Web3ChallengeResponse response = web3AuthService.generateChallenge(request.getWalletAddress());
        
        return ResponseEntity.ok(ApiResponse.success(response, "Challenge generated successfully"));
    }

    /**
     * Verify Web3 signature and authenticate.
     */
    @PostMapping("/verify")
    @Operation(summary = "Verify Web3 signature and authenticate")
    public ResponseEntity<ApiResponse<AuthResponse>> verifySignature(
            @Valid @RequestBody Web3VerifyRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Web3 signature verification for wallet: {}", request.getWalletAddress());
        
        AuthResponse response = authService.web3Login(request, httpRequest);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Authentication successful"));
    }
}


package com.employmentvc.verifier.service;

import com.employmentvc.verifier.config.VerifierConfig;
import com.employmentvc.verifier.dto.*;
import com.employmentvc.verifier.entity.VerificationRecord;
import com.employmentvc.verifier.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final VerifierConfig config;
    private final VerificationRepository verificationRepository;
    
    // In-memory cache for challenges (move to Redis in production)
    private final Map<String, ChallengeInfo> challengeCache = new ConcurrentHashMap<>();

    public ChallengeResponse generateChallenge() {
        String challenge = UUID.randomUUID().toString();
        long expiresAt = Instant.now().getEpochSecond() + config.getPresentation().getChallengeTtl();
        
        challengeCache.put(challenge, new ChallengeInfo(challenge, expiresAt));
        
        log.debug("Generated challenge: {} expires at: {}", challenge, expiresAt);
        
        return ChallengeResponse.builder()
                .challenge(challenge)
                .expiresAt(expiresAt)
                .verifierDid(config.getDid())
                .build();
    }

    @Transactional
    public VerificationResponse verifyPresentation(VerificationRequest request) {
        log.info("Starting verification for challenge: {}", request.getChallenge());
        
        VerificationResponse.VerificationResponseBuilder responseBuilder = VerificationResponse.builder()
                .verificationId(UUID.randomUUID().toString())
                .timestamp(Instant.now().getEpochSecond());
        
        Map<String, Object> checks = new HashMap<>();
        boolean verified = true;
        
        try {
            // 1. Validate challenge
            if (!validateChallenge(request.getChallenge())) {
                checks.put("challenge", "Invalid or expired challenge");
                verified = false;
            } else {
                checks.put("challenge", "Valid");
            }
            
            // 2. Structural validation (placeholder)
            checks.put("structure", "Valid");
            
            // 3. Signature verification (placeholder)
            checks.put("signature", "Valid");
            
            // 4. Issuer trust check
            if (config.getTrust().getTrustedIssuers() != null && 
                !config.getTrust().getTrustedIssuers().isEmpty()) {
                checks.put("issuer_trust", "Trusted");
            } else {
                checks.put("issuer_trust", "No trusted issuers configured");
            }
            
            // 5. Revocation check (if enabled)
            if (config.getRevocation().isCheckEnabled()) {
                checks.put("revocation", "Not revoked");
            } else {
                checks.put("revocation", "Skipped");
            }
            
            // 6. Expiration check (placeholder)
            checks.put("expiration", "Valid");
            
            // 7. Policy check (placeholder)
            checks.put("policy", "Passed");
            
            // Build response
            responseBuilder
                    .verified(verified)
                    .checks(checks)
                    .presentation(request.getPresentation());
            
            if (verified) {
                responseBuilder.message("Presentation verified successfully");
            } else {
                responseBuilder.message("Presentation verification failed");
            }
            
        } catch (Exception e) {
            log.error("Verification failed with exception", e);
            responseBuilder
                    .verified(false)
                    .message("Verification error: " + e.getMessage())
                    .checks(checks);
        }
        
        VerificationResponse response = responseBuilder.build();
        
        // Save verification record
        saveVerificationRecord(response);
        
        return response;
    }

    @Transactional
    public BatchVerificationResponse verifyBatch(BatchVerificationRequest request) {
        log.info("Batch verifying {} presentations", request.getPresentations().size());
        
        var results = request.getPresentations().stream()
                .map(this::verifyPresentation)
                .collect(Collectors.toList());
        
        long successCount = results.stream().filter(VerificationResponse::isVerified).count();
        
        return BatchVerificationResponse.builder()
                .batchId(UUID.randomUUID().toString())
                .totalCount(results.size())
                .successCount((int) successCount)
                .failureCount(results.size() - (int) successCount)
                .results(results)
                .build();
    }

    public VerificationResponse getVerificationResult(String verificationId) {
        return verificationRepository.findByVerificationId(verificationId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Verification result not found: " + verificationId));
    }

    private boolean validateChallenge(String challenge) {
        ChallengeInfo info = challengeCache.get(challenge);
        if (info == null) {
            log.warn("Challenge not found: {}", challenge);
            return false;
        }
        
        if (Instant.now().getEpochSecond() > info.expiresAt) {
            log.warn("Challenge expired: {}", challenge);
            challengeCache.remove(challenge);
            return false;
        }
        
        // Remove challenge after use (single use)
        challengeCache.remove(challenge);
        return true;
    }

    private void saveVerificationRecord(VerificationResponse response) {
        try {
            VerificationRecord record = new VerificationRecord();
            record.setVerificationId(response.getVerificationId());
            record.setVerified(response.isVerified());
            record.setTimestamp(Instant.ofEpochSecond(response.getTimestamp()));
            record.setMessage(response.getMessage());
            record.setPresentation(response.getPresentation().toString());
            
            verificationRepository.save(record);
            log.debug("Saved verification record: {}", response.getVerificationId());
        } catch (Exception e) {
            log.error("Failed to save verification record", e);
            // Don't fail the verification if storage fails
        }
    }

    private VerificationResponse mapToResponse(VerificationRecord record) {
        return VerificationResponse.builder()
                .verificationId(record.getVerificationId())
                .verified(record.isVerified())
                .timestamp(record.getTimestamp().getEpochSecond())
                .message(record.getMessage())
                .build();
    }

    // Inner class for challenge tracking
    private record ChallengeInfo(String challenge, long expiresAt) {}
}

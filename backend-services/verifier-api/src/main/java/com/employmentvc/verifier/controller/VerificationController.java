package com.employmentvc.verifier.controller;

import com.employmentvc.verifier.dto.ChallengeResponse;
import com.employmentvc.verifier.dto.VerificationRequest;
import com.employmentvc.verifier.dto.VerificationResponse;
import com.employmentvc.verifier.dto.BatchVerificationRequest;
import com.employmentvc.verifier.dto.BatchVerificationResponse;
import com.employmentvc.verifier.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/verifier")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Verification", description = "Verifiable Presentation verification endpoints")
public class VerificationController {

    private final VerificationService verificationService;

    @GetMapping("/challenge")
    // `@Operation` is an annotation provided by Swagger (OpenAPI) that is used to document API
    // operations. It allows you to provide a summary and description for each operation in your API.
    // This annotation helps in generating API documentation automatically based on the provided
    // information.
    @Operation(summary = "Generate a verification challenge", 
               description = "Creates a unique challenge for presentation submission")
    public ResponseEntity<ChallengeResponse> generateChallenge() {
        log.info("Generating verification challenge");
        ChallengeResponse challenge = verificationService.generateChallenge();
        return ResponseEntity.ok(challenge);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify a presentation", 
               description = "Verifies a verifiable presentation with cryptographic and policy checks")
    public ResponseEntity<VerificationResponse> verifyPresentation(
            @Valid @RequestBody VerificationRequest request) {
        log.info("Verifying presentation for challenge: {}", request.getChallenge());
        VerificationResponse response = verificationService.verifyPresentation(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify/batch")
    @Operation(summary = "Batch verify presentations", 
               description = "Verifies multiple presentations in a single request")
    public ResponseEntity<BatchVerificationResponse> verifyBatch(
            @Valid @RequestBody BatchVerificationRequest request) {
        log.info("Batch verifying {} presentations", request.getPresentations().size());
        BatchVerificationResponse response = verificationService.verifyBatch(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/result/{verificationId}")
    @Operation(summary = "Get verification result", 
               description = "Retrieves the result of a previous verification")
    public ResponseEntity<VerificationResponse> getVerificationResult(
            @PathVariable String verificationId) {
        log.info("Retrieving verification result for ID: {}", verificationId);
        VerificationResponse response = verificationService.getVerificationResult(verificationId);
        return ResponseEntity.ok(response);
    }
}

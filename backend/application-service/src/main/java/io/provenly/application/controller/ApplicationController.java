package io.provenly.application.controller;

import io.provenly.application.issuer.service.CredentialIssuerService;
import io.provenly.application.verifier.service.CredentialVerifierService;
import io.provenly.application.wallet.service.CustodialWalletService;
import io.provenly.application.wallet.service.NonCustodialWalletService;
import io.provenly.application.issuer.dto.*;
import io.provenly.application.verifier.dto.*;
import io.provenly.application.wallet.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.UUID;

/**
 * Main REST controller for the Application Service.
 * Provides endpoints for credential issuance, verification, and wallet management.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Application Service", description = "Core VC operations - Issuer, Verifier, and Wallets")
public class ApplicationController {

    private final CredentialIssuerService issuerService;
    private final CredentialVerifierService verifierService;
    private final CustodialWalletService custodialWalletService;
    private final NonCustodialWalletService nonCustodialWalletService;

    // ================================
    // CREDENTIAL ISSUANCE ENDPOINTS
    // ================================

    @Operation(summary = "Issue a new Verifiable Credential")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Credential issued successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/credentials/issue")
    @PreAuthorize("hasRole('ISSUER') or hasRole('ADMIN')")
    public ResponseEntity<IssueCredentialResponse> issueCredential(
            @Valid @RequestBody IssueCredentialRequest request) {
        log.info("Issuing credential for subject: {}", request.getSubjectDid());
        IssueCredentialResponse response = issuerService.issueCredential(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Revoke a Verifiable Credential")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Credential revoked successfully"),
        @ApiResponse(responseCode = "404", description = "Credential not found"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to revoke")
    })
    @PostMapping("/credentials/revoke")
    @PreAuthorize("hasRole('ISSUER') or hasRole('ADMIN')")
    public ResponseEntity<Void> revokeCredential(
            @Valid @RequestBody RevokeCredentialRequest request) {
        log.info("Revoking credential: {}", request.getCredentialId());
        issuerService.revokeCredential(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get issued credentials for an issuer")
    @GetMapping("/credentials/issued")
    @PreAuthorize("hasRole('ISSUER') or hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<CredentialSummary>> getIssuedCredentials(
            @Parameter(description = "Issuer DID") @RequestParam String issuerDid,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Getting issued credentials for issuer: {}", issuerDid);
        var credentials = issuerService.getIssuedCredentials(issuerDid, page, size);
        return ResponseEntity.ok(PagedResponse.of(credentials, page, size));
    }

    @Operation(summary = "Get credential by ID")
    @GetMapping("/credentials/{credentialId}")
    @PreAuthorize("hasRole('ISSUER') or hasRole('VERIFIER') or hasRole('HOLDER') or hasRole('ADMIN')")
    public ResponseEntity<CredentialResponse> getCredential(
            @Parameter(description = "Credential ID") @PathVariable String credentialId) {
        log.info("Getting credential: {}", credentialId);
        var credential = issuerService.getCredential(credentialId);
        return ResponseEntity.ok(CredentialResponse.from(credential));
    }

    // ================================
    // CREDENTIAL VERIFICATION ENDPOINTS
    // ================================

    @Operation(summary = "Verify a Verifiable Credential")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verification completed"),
        @ApiResponse(responseCode = "400", description = "Invalid credential format")
    })
    @PostMapping("/credentials/verify")
    @PreAuthorize("hasRole('VERIFIER') or hasRole('ADMIN')")
    public ResponseEntity<VerifyCredentialResponse> verifyCredential(
            @Valid @RequestBody VerifyCredentialRequest request) {
        log.info("Verifying credential: {}", request.getCredential().get("id"));
        VerifyCredentialResponse response = verifierService.verifyCredential(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verify a Verifiable Presentation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verification completed"),
        @ApiResponse(responseCode = "400", description = "Invalid presentation format")
    })
    @PostMapping("/presentations/verify")
    @PreAuthorize("hasRole('VERIFIER') or hasRole('ADMIN')")
    public ResponseEntity<VerifyPresentationResponse> verifyPresentation(
            @Valid @RequestBody VerifyPresentationRequest request) {
        log.info("Verifying presentation from holder: {}", request.getPresentation().get("holder"));
        VerifyPresentationResponse response = verifierService.verifyPresentation(request);
        return ResponseEntity.ok(response);
    }

    // ================================
    // CUSTODIAL WALLET ENDPOINTS
    // ================================

    @Operation(summary = "Create a new custodial wallet")
    @PostMapping("/wallets/custodial")
    @PreAuthorize("hasRole('HOLDER') or hasRole('ADMIN')")
    public ResponseEntity<CreateWalletResponse> createCustodialWallet(
            @Valid @RequestBody CreateCustodialWalletRequest request) {
        log.info("Creating custodial wallet for owner: {}", request.getOwnerId());
        CreateWalletResponse response = custodialWalletService.createWallet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Store a credential in a custodial wallet")
    @PostMapping("/wallets/custodial/{walletId}/credentials")
    @PreAuthorize("hasRole('HOLDER') or hasRole('ADMIN')")
    public ResponseEntity<StoreCredentialResponse> storeCredentialInCustodialWallet(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId,
            @Valid @RequestBody StoreCredentialRequest request) {
        request.setWalletId(walletId);
        log.info("Storing credential in custodial wallet: {}", walletId);
        StoreCredentialResponse response = custodialWalletService.storeCredential(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get a credential from a custodial wallet")
    @GetMapping("/wallets/custodial/{walletId}/credentials/{credentialId}")
    @PreAuthorize("hasRole('HOLDER') or hasRole('ADMIN')")
    public ResponseEntity<GetCredentialResponse> getCredentialFromCustodialWallet(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId,
            @Parameter(description = "Credential ID") @PathVariable String credentialId,
            @Parameter(description = "Owner ID") @RequestParam String ownerId) {
        log.info("Getting credential {} from custodial wallet: {}", credentialId, walletId);
        GetCredentialRequest request = GetCredentialRequest.builder()
            .walletId(walletId)
            .credentialId(credentialId)
            .ownerId(ownerId)
            .build();
        GetCredentialResponse response = custodialWalletService.getCredential(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List credentials in a custodial wallet")
    @GetMapping("/wallets/custodial/{walletId}/credentials")
    @PreAuthorize("hasRole('HOLDER') or hasRole('ADMIN')")
    public ResponseEntity<ListCredentialsResponse> listCustodialWalletCredentials(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId,
            @Parameter(description = "Owner ID") @RequestParam String ownerId,
            @Parameter(description = "Credential type filter") @RequestParam(required = false) String type,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Listing credentials in custodial wallet: {}", walletId);
        ListCredentialsRequest request = ListCredentialsRequest.builder()
            .walletId(walletId)
            .ownerId(ownerId)
            .type(type)
            .page(page)
            .size(size)
            .build();
        ListCredentialsResponse response = custodialWalletService.listCredentials(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create a presentation from custodial wallet credentials")
    @PostMapping("/wallets/custodial/{walletId}/presentations")
    @PreAuthorize("hasRole('HOLDER') or hasRole('ADMIN')")
    public ResponseEntity<CreatePresentationResponse> createPresentationFromCustodialWallet(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId,
            @Valid @RequestBody CreatePresentationRequest request) {
        request.setWalletId(walletId);
        log.info("Creating presentation from custodial wallet: {}", walletId);
        CreatePresentationResponse response = custodialWalletService.createPresentation(request);
        return ResponseEntity.ok(response);
    }

    // ================================
    // NON-CUSTODIAL WALLET ENDPOINTS
    // ================================

    @Operation(summary = "Register a non-custodial wallet")
    @PostMapping("/wallets/non-custodial")
    @PreAuthorize("hasRole('HOLDER') or hasRole('ADMIN')")
    public ResponseEntity<CreateWalletResponse> registerNonCustodialWallet(
            @Valid @RequestBody RegisterNonCustodialWalletRequest request) {
        log.info("Registering non-custodial wallet for owner: {}", request.getOwnerId());
        CreateWalletResponse response = nonCustodialWalletService.registerWallet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Register credential metadata for non-custodial wallet")
    @PostMapping("/wallets/non-custodial/{walletId}/credentials/metadata")
    @PreAuthorize("hasRole('HOLDER') or hasRole('ADMIN')")
    public ResponseEntity<RegisterCredentialMetadataResponse> registerCredentialMetadata(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId,
            @Valid @RequestBody RegisterCredentialMetadataRequest request) {
        request.setWalletId(walletId);
        log.info("Registering credential metadata for non-custodial wallet: {}", walletId);
        RegisterCredentialMetadataResponse response = nonCustodialWalletService
            .registerCredentialMetadata(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List credential metadata in non-custodial wallet")
    @GetMapping("/wallets/non-custodial/{walletId}/credentials")
    @PreAuthorize("hasRole('HOLDER') or hasRole('ADMIN')")
    public ResponseEntity<ListCredentialsResponse> listNonCustodialWalletCredentials(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId,
            @Parameter(description = "Owner ID") @RequestParam String ownerId,
            @Parameter(description = "Credential type filter") @RequestParam(required = false) String type,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Listing credential metadata in non-custodial wallet: {}", walletId);
        ListCredentialsRequest request = ListCredentialsRequest.builder()
            .walletId(walletId)
            .ownerId(ownerId)
            .type(type)
            .page(page)
            .size(size)
            .build();
        ListCredentialsResponse response = nonCustodialWalletService.listCredentialMetadata(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verify user-created presentation from non-custodial wallet")
    @PostMapping("/wallets/non-custodial/{walletId}/presentations/verify")
    @PreAuthorize("hasRole('HOLDER') or hasRole('VERIFIER') or hasRole('ADMIN')")
    public ResponseEntity<VerifyPresentationResponse> verifyUserPresentation(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId,
            @Valid @RequestBody VerifyUserPresentationRequest request) {
        request.setWalletId(walletId);
        log.info("Verifying user presentation for non-custodial wallet: {}", walletId);
        VerifyPresentationResponse response = nonCustodialWalletService.verifyUserPresentation(request);
        return ResponseEntity.ok(response);
    }

    // ================================
    // COMMON WALLET ENDPOINTS
    // ================================

    @Operation(summary = "Get wallet information")
    @GetMapping("/wallets/{walletId}")
    @PreAuthorize("hasRole('HOLDER') or hasRole('ADMIN')")
    public ResponseEntity<WalletInfoResponse> getWalletInfo(
            @Parameter(description = "Wallet ID") @PathVariable UUID walletId,
            @Parameter(description = "Owner ID") @RequestParam String ownerId) {
        log.info("Getting wallet info: {}", walletId);
        
        // Try custodial first, then non-custodial
        try {
            WalletInfoResponse response = custodialWalletService.getWalletInfo(walletId, ownerId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            WalletInfoResponse response = nonCustodialWalletService.getWalletInfo(walletId, ownerId);
            return ResponseEntity.ok(response);
        }
    }

    // ================================
    // HEALTH AND STATUS ENDPOINTS
    // ================================

    @Operation(summary = "Get service health status")
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        return ResponseEntity.ok(HealthResponse.builder()
            .status("healthy")
            .timestamp(java.time.Instant.now())
            .service("application-service")
            .version("1.0.0")
            .build());
    }

    @Operation(summary = "Get service metrics")
    @GetMapping("/metrics")
    public ResponseEntity<MetricsResponse> getMetrics() {
        // Implementation would include actual metrics
        return ResponseEntity.ok(MetricsResponse.builder()
            .credentialsIssued(0L)
            .credentialsVerified(0L)
            .walletsCreated(0L)
            .presentationsCreated(0L)
            .build());
    }
}

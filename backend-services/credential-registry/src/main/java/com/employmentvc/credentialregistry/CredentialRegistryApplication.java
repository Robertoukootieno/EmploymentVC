package com.employmentvc.credentialregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Credential Registry & Revocation Service
 * 
 * Responsibilities:
 * - Track all issued verifiable credentials
 * - Manage credential lifecycle (active, revoked, expired)
 * - Implement revocation mechanisms (Status List 2021, Revocation List)
 * - Provide credential status endpoints for verifiers
 * - Audit trail for credential operations
 * - Integration with DID ledger for revocation anchoring
 * 
 * Standards: W3C VC Status List 2021, Revocation List 2020
 * Architecture: Event-sourced credential state
 */
@SpringBootApplication
public class CredentialRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(CredentialRegistryApplication.class, args);
    }
}

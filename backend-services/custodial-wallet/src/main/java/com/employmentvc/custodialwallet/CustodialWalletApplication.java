package com.employmentvc.custodialwallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Custodial Wallet Service
 * 
 * Responsibilities:
 * - Manage platform-controlled wallets for holders
 * - Secure key generation and storage (HSM/Vault)
 * - Credential storage and retrieval
 * - Presentation creation with selective disclosure
 * - Backup and recovery mechanisms
 * - Web and mobile wallet interfaces
 * 
 * Security Model: Platform manages keys, user controls access
 * Key Storage: HashiCorp Vault / Cloud HSM
 * Standards: W3C VC Data Model, DID Core, SD-JWT
 */
@SpringBootApplication
public class CustodialWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustodialWalletApplication.class, args);
    }
}

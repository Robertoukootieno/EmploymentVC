package com.employmentvc.noncustodialgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Non-Custodial Wallet Gateway
 * 
 * Responsibilities:
 * - Integration with external wallets (mobile wallets, browser extensions)
 * - DIDComm protocol support for wallet communication
 * - OpenID4VC flow orchestration
 * - Presentation request/response handling
 * - Keys NEVER leave holder control
 * - Protocol translation and routing
 * 
 * Supported Protocols:
 * - DIDComm v2
 * - OpenID4VC (SIOP, VP Request)
 * - CHAPI (Credential Handler API)
 * - Wallet Connect (Web3)
 * 
 * Security: Zero-knowledge of holder keys
 */
@SpringBootApplication
public class NonCustodialGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(NonCustodialGatewayApplication.class, args);
    }
}

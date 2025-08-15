package io.provenly.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Application Service for Provenly Employment VC Platform.
 * 
 * This service provides core Verifiable Credential operations including:
 * - Credential Issuance (Issuer)
 * - Credential Verification (Verifier) 
 * - Custodial Wallet Management
 * - Non-Custodial Wallet Support
 * - Selective Disclosure Operations
 * - JSON-LD Processing
 * - EBSI Integration
 * - Walt.id Integration
 * 
 * @author Provenly Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@ConfigurationPropertiesScan
public class ApplicationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationServiceApplication.class, args);
    }
}

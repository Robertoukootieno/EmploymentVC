package io.provenly.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Provenly Wallet API Application.
 * 
 * Provides wallet management services for custodial and non-custodial wallets,
 * credential storage, and verifiable presentation generation with selective disclosure.
 */
@SpringBootApplication(scanBasePackages = {
    "io.provenly.wallet",
    "io.provenly.commons",
    "io.provenly.crypto"
})
@EnableCaching
@EnableAsync
@EnableTransactionManagement
@EnableMethodSecurity(prePostEnabled = true)
@ConfigurationPropertiesScan
public class WalletApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletApiApplication.class, args);
    }
}


package io.provenly.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Provenly Auth Service Application.
 * 
 * Provides authentication and authorization services with Keycloak integration,
 * Web3 wallet authentication, and traditional username/password authentication.
 */
@SpringBootApplication(scanBasePackages = {
    "io.provenly.auth",
    "io.provenly.commons"
})
@EnableCaching
@EnableAsync
@EnableTransactionManagement
@EnableMethodSecurity(prePostEnabled = true)
@ConfigurationPropertiesScan
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}

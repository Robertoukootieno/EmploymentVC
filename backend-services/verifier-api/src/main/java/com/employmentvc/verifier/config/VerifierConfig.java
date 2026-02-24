package com.employmentvc.verifier.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "verifier")
@Data
public class VerifierConfig {
    
    private String did;
    
    private PresentationConfig presentation = new PresentationConfig();
    
    private TrustConfig trust = new TrustConfig();
    
    private RevocationConfig revocation = new RevocationConfig();
    
    @Data
    public static class PresentationConfig {
        private int challengeTtl = 600; // 10 minutes default
    }
    
    @Data
    public static class TrustConfig {
        private List<String> trustedIssuers;
    }
    
    @Data
    public static class RevocationConfig {
        private boolean checkEnabled = true;
        private int cacheTtl = 300; // 5 minutes default
    }
}

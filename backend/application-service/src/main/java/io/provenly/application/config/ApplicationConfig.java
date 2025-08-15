package io.provenly.application.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.WebClient;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.concurrent.Executor;

/**
 * Main configuration class for the Application Service.
 * Configures beans for Web3, Redis, HTTP clients, and other core components.
 */
@Configuration
public class ApplicationConfig {

    /**
     * Configure ObjectMapper for JSON processing with proper date handling.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return mapper;
    }

    /**
     * Configure Web3j client for blockchain interactions.
     */
    @Bean
    public Web3j web3j(BlockchainProperties blockchainProperties) {
        return Web3j.build(new HttpService(blockchainProperties.getRpcUrl()));
    }

    /**
     * Configure WebClient for HTTP requests to external services.
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    /**
     * Configure Redis template for caching and session management.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Configure async task executor for background processing.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("provenly-async-");
        executor.initialize();
        return executor;
    }

    /**
     * Blockchain configuration properties.
     */
    @ConfigurationProperties(prefix = "provenly.blockchain")
    public static class BlockchainProperties {
        private String rpcUrl = "http://localhost:8545";
        private String wsUrl = "ws://localhost:8546";
        private int networkId = 1337;
        private int chainId = 1337;
        private String privateKey;

        // Getters and setters
        public String getRpcUrl() { return rpcUrl; }
        public void setRpcUrl(String rpcUrl) { this.rpcUrl = rpcUrl; }
        
        public String getWsUrl() { return wsUrl; }
        public void setWsUrl(String wsUrl) { this.wsUrl = wsUrl; }
        
        public int getNetworkId() { return networkId; }
        public void setNetworkId(int networkId) { this.networkId = networkId; }
        
        public int getChainId() { return chainId; }
        public void setChainId(int chainId) { this.chainId = chainId; }
        
        public String getPrivateKey() { return privateKey; }
        public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    }

    /**
     * EBSI configuration properties.
     */
    @ConfigurationProperties(prefix = "provenly.ebsi")
    public static class EbsiProperties {
        private String apiBaseUrl = "https://api-pilot.ebsi.eu";
        private String didRegistryUrl = "https://api-pilot.ebsi.eu/did-registry/v4";
        private String trustedIssuersRegistryUrl = "https://api-pilot.ebsi.eu/trusted-issuers-registry/v4";
        private String trustedSchemasRegistryUrl = "https://api-pilot.ebsi.eu/trusted-schemas-registry/v2";
        private String clientId;
        private String clientSecret;
        private String privateKey;

        // Getters and setters
        public String getApiBaseUrl() { return apiBaseUrl; }
        public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
        
        public String getDidRegistryUrl() { return didRegistryUrl; }
        public void setDidRegistryUrl(String didRegistryUrl) { this.didRegistryUrl = didRegistryUrl; }
        
        public String getTrustedIssuersRegistryUrl() { return trustedIssuersRegistryUrl; }
        public void setTrustedIssuersRegistryUrl(String trustedIssuersRegistryUrl) { 
            this.trustedIssuersRegistryUrl = trustedIssuersRegistryUrl; 
        }
        
        public String getTrustedSchemasRegistryUrl() { return trustedSchemasRegistryUrl; }
        public void setTrustedSchemasRegistryUrl(String trustedSchemasRegistryUrl) { 
            this.trustedSchemasRegistryUrl = trustedSchemasRegistryUrl; 
        }
        
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        
        public String getPrivateKey() { return privateKey; }
        public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    }

    /**
     * Walt.id configuration properties.
     */
    @ConfigurationProperties(prefix = "provenly.waltid")
    public static class WaltIdProperties {
        private String coreApiUrl = "http://localhost:7000";
        private String signatoryApiUrl = "http://localhost:7001";
        private String custodianApiUrl = "http://localhost:7002";
        private String auditorApiUrl = "http://localhost:7003";
        private String apiKey;

        // Getters and setters
        public String getCoreApiUrl() { return coreApiUrl; }
        public void setCoreApiUrl(String coreApiUrl) { this.coreApiUrl = coreApiUrl; }
        
        public String getSignatoryApiUrl() { return signatoryApiUrl; }
        public void setSignatoryApiUrl(String signatoryApiUrl) { this.signatoryApiUrl = signatoryApiUrl; }
        
        public String getCustodianApiUrl() { return custodianApiUrl; }
        public void setCustodianApiUrl(String custodianApiUrl) { this.custodianApiUrl = custodianApiUrl; }
        
        public String getAuditorApiUrl() { return auditorApiUrl; }
        public void setAuditorApiUrl(String auditorApiUrl) { this.auditorApiUrl = auditorApiUrl; }
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    /**
     * Selective Disclosure configuration properties.
     */
    @ConfigurationProperties(prefix = "provenly.selective-disclosure")
    public static class SelectiveDisclosureProperties {
        private String issuerKey;
        private boolean holderBindingRequired = true;
        private String defaultSuite = "BbsBlsSignature2020";

        // Getters and setters
        public String getIssuerKey() { return issuerKey; }
        public void setIssuerKey(String issuerKey) { this.issuerKey = issuerKey; }
        
        public boolean isHolderBindingRequired() { return holderBindingRequired; }
        public void setHolderBindingRequired(boolean holderBindingRequired) { 
            this.holderBindingRequired = holderBindingRequired; 
        }
        
        public String getDefaultSuite() { return defaultSuite; }
        public void setDefaultSuite(String defaultSuite) { this.defaultSuite = defaultSuite; }
    }

    @Bean
    @ConfigurationProperties(prefix = "provenly.blockchain")
    public BlockchainProperties blockchainProperties() {
        return new BlockchainProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "provenly.ebsi")
    public EbsiProperties ebsiProperties() {
        return new EbsiProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "provenly.waltid")
    public WaltIdProperties waltIdProperties() {
        return new WaltIdProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "provenly.selective-disclosure")
    public SelectiveDisclosureProperties selectiveDisclosureProperties() {
        return new SelectiveDisclosureProperties();
    }
}

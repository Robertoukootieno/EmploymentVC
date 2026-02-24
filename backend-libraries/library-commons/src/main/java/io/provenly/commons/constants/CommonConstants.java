package io.provenly.commons.constants;

/**
 * Common constants used across the platform.
 */
public final class CommonConstants {

    private CommonConstants() {
        // Utility class - prevent instantiation
    }

    /**
     * API version constants.
     */
    public static final class Api {
        public static final String VERSION_V1 = "v1";
        public static final String BASE_PATH = "/api";
        public static final String V1_PATH = BASE_PATH + "/" + VERSION_V1;
        
        private Api() {}
    }

    /**
     * HTTP header constants.
     */
    public static final class Headers {
        public static final String AUTHORIZATION = "Authorization";
        public static final String BEARER_PREFIX = "Bearer ";
        public static final String CORRELATION_ID = "X-Correlation-ID";
        public static final String REQUEST_ID = "X-Request-ID";
        public static final String API_KEY = "X-API-Key";
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String ACCEPT = "Accept";
        
        private Headers() {}
    }

    /**
     * Content type constants.
     */
    public static final class ContentTypes {
        public static final String APPLICATION_JSON = "application/json";
        public static final String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
        public static final String APPLICATION_LD_JSON = "application/ld+json";
        public static final String APPLICATION_JWT = "application/jwt";
        public static final String TEXT_PLAIN = "text/plain";
        
        private ContentTypes() {}
    }

    /**
     * Date/time format constants.
     */
    public static final class DateFormats {
        public static final String ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        public static final String ISO_8601_DATE = "yyyy-MM-dd";
        public static final String ISO_8601_TIME = "HH:mm:ss";
        
        private DateFormats() {}
    }

    /**
     * Pagination constants.
     */
    public static final class Pagination {
        public static final int DEFAULT_PAGE = 0;
        public static final int DEFAULT_SIZE = 20;
        public static final int MAX_SIZE = 100;
        public static final String PAGE_PARAM = "page";
        public static final String SIZE_PARAM = "size";
        public static final String SORT_PARAM = "sort";
        
        private Pagination() {}
    }

    /**
     * Cache constants.
     */
    public static final class Cache {
        public static final String CREDENTIALS = "credentials";
        public static final String WALLETS = "wallets";
        public static final String DIDS = "dids";
        public static final String SCHEMAS = "schemas";
        public static final String USERS = "users";
        public static final int DEFAULT_TTL_SECONDS = 3600; // 1 hour
        
        private Cache() {}
    }

    /**
     * Security constants.
     */
    public static final class Security {
        public static final String ROLE_PREFIX = "ROLE_";
        public static final String ROLE_USER = "ROLE_USER";
        public static final String ROLE_ISSUER = "ROLE_ISSUER";
        public static final String ROLE_VERIFIER = "ROLE_VERIFIER";
        public static final String ROLE_ADMIN = "ROLE_ADMIN";
        
        public static final int JWT_EXPIRATION_HOURS = 24;
        public static final int REFRESH_TOKEN_EXPIRATION_DAYS = 30;
        public static final int PASSWORD_MIN_LENGTH = 8;
        
        private Security() {}
    }

    /**
     * Credential constants.
     */
    public static final class Credentials {
        public static final String CONTEXT_W3C_VC = "https://www.w3.org/2018/credentials/v1";
        public static final String TYPE_VERIFIABLE_CREDENTIAL = "VerifiableCredential";
        public static final String TYPE_VERIFIABLE_PRESENTATION = "VerifiablePresentation";
        public static final String TYPE_EMPLOYMENT_CREDENTIAL = "EmploymentCredential";
        
        public static final String PROOF_TYPE_ED25519 = "Ed25519Signature2020";
        public static final String PROOF_TYPE_ECDSA = "EcdsaSecp256k1Signature2019";
        public static final String PROOF_TYPE_BBS = "BbsBlsSignature2020";
        
        private Credentials() {}
    }

    /**
     * DID constants.
     */
    public static final class Did {
        public static final String METHOD_EBSI = "ebsi";
        public static final String METHOD_KEY = "key";
        public static final String METHOD_WEB = "web";
        public static final String METHOD_ION = "ion";
        
        public static final String KEY_TYPE_ED25519 = "Ed25519";
        public static final String KEY_TYPE_SECP256K1 = "Secp256k1";
        public static final String KEY_TYPE_RSA = "RSA";
        
        private Did() {}
    }

    /**
     * Wallet constants.
     */
    public static final class Wallet {
        public static final String TYPE_CUSTODIAL = "CUSTODIAL";
        public static final String TYPE_NON_CUSTODIAL = "NON_CUSTODIAL";
        
        public static final String STATUS_ACTIVE = "ACTIVE";
        public static final String STATUS_INACTIVE = "INACTIVE";
        public static final String STATUS_LOCKED = "LOCKED";
        
        private Wallet() {}
    }

    /**
     * Error code constants.
     */
    public static final class ErrorCodes {
        public static final String GENERAL_ERROR = "GENERAL_ERROR";
        public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
        public static final String AUTH_ERROR = "AUTH_ERROR";
        public static final String AUTHZ_ERROR = "AUTHZ_ERROR";
        public static final String NOT_FOUND = "NOT_FOUND";
        public static final String CONFLICT = "CONFLICT";
        public static final String CREDENTIAL_ERROR = "CREDENTIAL_ERROR";
        public static final String WALLET_ERROR = "WALLET_ERROR";
        public static final String CRYPTO_ERROR = "CRYPTO_ERROR";
        public static final String DID_ERROR = "DID_ERROR";
        
        private ErrorCodes() {}
    }
}


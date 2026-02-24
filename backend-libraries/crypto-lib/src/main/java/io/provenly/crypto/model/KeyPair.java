package io.provenly.crypto.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a cryptographic key pair.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyPair {
    
    /**
     * The public key in various formats.
     */
    private String publicKeyHex;
    private String publicKeyBase64;
    private String publicKeyPem;
    private byte[] publicKeyBytes;
    
    /**
     * The private key in various formats.
     */
    private String privateKeyHex;
    private String privateKeyBase64;
    private String privateKeyPem;
    private byte[] privateKeyBytes;
    
    /**
     * Key metadata.
     */
    private KeyType keyType;
    private String keyId;
    private String algorithm;
    
    /**
     * Check if this key pair has a private key.
     */
    public boolean hasPrivateKey() {
        return privateKeyBytes != null && privateKeyBytes.length > 0;
    }
    
    /**
     * Check if this is a public-only key.
     */
    public boolean isPublicOnly() {
        return !hasPrivateKey();
    }
}


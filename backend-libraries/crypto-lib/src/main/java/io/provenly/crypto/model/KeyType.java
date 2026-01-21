package io.provenly.crypto.model;

/**
 * Supported cryptographic key types.
 */
public enum KeyType {
    /**
     * RSA key (2048, 3072, or 4096 bits).
     */
    RSA,
    
    /**
     * Ed25519 elliptic curve key (EdDSA).
     */
    ED25519,
    
    /**
     * SECP256K1 elliptic curve key (used in Bitcoin/Ethereum).
     */
    SECP256K1,
    
    /**
     * SECP256R1 (P-256) elliptic curve key.
     */
    SECP256R1,
    
    /**
     * SECP384R1 (P-384) elliptic curve key.
     */
    SECP384R1,
    
    /**
     * SECP521R1 (P-521) elliptic curve key.
     */
    SECP521R1,
    
    /**
     * BLS12-381 key (for BBS+ signatures).
     */
    BLS12_381;
    
    /**
     * Get the algorithm name for this key type.
     */
    public String getAlgorithm() {
        return switch (this) {
            case RSA -> "RSA";
            case ED25519 -> "Ed25519";
            case SECP256K1 -> "secp256k1";
            case SECP256R1 -> "secp256r1";
            case SECP384R1 -> "secp384r1";
            case SECP521R1 -> "secp521r1";
            case BLS12_381 -> "BLS12-381";
        };
    }
    
    /**
     * Check if this is an elliptic curve key type.
     */
    public boolean isEllipticCurve() {
        return this != RSA;
    }
}


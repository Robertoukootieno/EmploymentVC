package io.provenly.crypto.service;

import io.provenly.commons.util.EncodingUtils;
import io.provenly.crypto.model.KeyPair;
import io.provenly.crypto.model.KeyType;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.UUID;

/**
 * Service for generating cryptographic key pairs.
 */
@Slf4j
public class KeyGenerationService {

    static {
        // Register Bouncy Castle provider
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Generate a key pair of the specified type.
     */
    public KeyPair generateKeyPair(KeyType keyType) {
        return switch (keyType) {
            case RSA -> generateRsaKeyPair(2048);
            case ED25519 -> generateEd25519KeyPair();
            case SECP256K1 -> generateSecp256k1KeyPair();
            case SECP256R1 -> generateEcKeyPair("secp256r1");
            case SECP384R1 -> generateEcKeyPair("secp384r1");
            case SECP521R1 -> generateEcKeyPair("secp521r1");
            case BLS12_381 -> throw new UnsupportedOperationException("BLS12-381 not yet implemented");
        };
    }

    /**
     * Generate an RSA key pair.
     */
    private KeyPair generateRsaKeyPair(int keySize) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(keySize, new SecureRandom());
            java.security.KeyPair javaKeyPair = generator.generateKeyPair();

            return KeyPair.builder()
                    .keyType(KeyType.RSA)
                    .keyId(UUID.randomUUID().toString())
                    .algorithm("RSA")
                    .publicKeyBytes(javaKeyPair.getPublic().getEncoded())
                    .publicKeyHex(EncodingUtils.encodeHex(javaKeyPair.getPublic().getEncoded()))
                    .publicKeyBase64(EncodingUtils.encodeBase64(javaKeyPair.getPublic().getEncoded()))
                    .privateKeyBytes(javaKeyPair.getPrivate().getEncoded())
                    .privateKeyHex(EncodingUtils.encodeHex(javaKeyPair.getPrivate().getEncoded()))
                    .privateKeyBase64(EncodingUtils.encodeBase64(javaKeyPair.getPrivate().getEncoded()))
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate RSA key pair", e);
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    /**
     * Generate an Ed25519 key pair.
     */
    private KeyPair generateEd25519KeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519", "BC");
            java.security.KeyPair javaKeyPair = generator.generateKeyPair();

            return KeyPair.builder()
                    .keyType(KeyType.ED25519)
                    .keyId(UUID.randomUUID().toString())
                    .algorithm("Ed25519")
                    .publicKeyBytes(javaKeyPair.getPublic().getEncoded())
                    .publicKeyHex(EncodingUtils.encodeHex(javaKeyPair.getPublic().getEncoded()))
                    .publicKeyBase64(EncodingUtils.encodeBase64(javaKeyPair.getPublic().getEncoded()))
                    .privateKeyBytes(javaKeyPair.getPrivate().getEncoded())
                    .privateKeyHex(EncodingUtils.encodeHex(javaKeyPair.getPrivate().getEncoded()))
                    .privateKeyBase64(EncodingUtils.encodeBase64(javaKeyPair.getPrivate().getEncoded()))
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate Ed25519 key pair", e);
            throw new RuntimeException("Failed to generate Ed25519 key pair", e);
        }
    }

    /**
     * Generate a SECP256K1 key pair using Web3j.
     */
    private KeyPair generateSecp256k1KeyPair() {
        try {
            ECKeyPair ecKeyPair = Keys.createEcKeyPair();

            byte[] publicKeyBytes = ecKeyPair.getPublicKey().toByteArray();
            byte[] privateKeyBytes = ecKeyPair.getPrivateKey().toByteArray();

            return KeyPair.builder()
                    .keyType(KeyType.SECP256K1)
                    .keyId(UUID.randomUUID().toString())
                    .algorithm("secp256k1")
                    .publicKeyBytes(publicKeyBytes)
                    .publicKeyHex(EncodingUtils.encodeHex(publicKeyBytes))
                    .publicKeyBase64(EncodingUtils.encodeBase64(publicKeyBytes))
                    .privateKeyBytes(privateKeyBytes)
                    .privateKeyHex(EncodingUtils.encodeHex(privateKeyBytes))
                    .privateKeyBase64(EncodingUtils.encodeBase64(privateKeyBytes))
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate SECP256K1 key pair", e);
            throw new RuntimeException("Failed to generate SECP256K1 key pair", e);
        }
    }

    /**
     * Generate an elliptic curve key pair.
     */
    private KeyPair generateEcKeyPair(String curveName) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", "BC");
            generator.initialize(new ECGenParameterSpec(curveName), new SecureRandom());
            java.security.KeyPair javaKeyPair = generator.generateKeyPair();

            KeyType keyType = switch (curveName) {
                case "secp256r1" -> KeyType.SECP256R1;
                case "secp384r1" -> KeyType.SECP384R1;
                case "secp521r1" -> KeyType.SECP521R1;
                default -> throw new IllegalArgumentException("Unsupported curve: " + curveName);
            };

            return KeyPair.builder()
                    .keyType(keyType)
                    .keyId(UUID.randomUUID().toString())
                    .algorithm(curveName)
                    .publicKeyBytes(javaKeyPair.getPublic().getEncoded())
                    .publicKeyHex(EncodingUtils.encodeHex(javaKeyPair.getPublic().getEncoded()))
                    .publicKeyBase64(EncodingUtils.encodeBase64(javaKeyPair.getPublic().getEncoded()))
                    .privateKeyBytes(javaKeyPair.getPrivate().getEncoded())
                    .privateKeyHex(EncodingUtils.encodeHex(javaKeyPair.getPrivate().getEncoded()))
                    .privateKeyBase64(EncodingUtils.encodeBase64(javaKeyPair.getPrivate().getEncoded()))
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate EC key pair for curve: {}", curveName, e);
            throw new RuntimeException("Failed to generate EC key pair", e);
        }
    }
}


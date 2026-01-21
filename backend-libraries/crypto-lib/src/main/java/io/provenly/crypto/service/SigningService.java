package io.provenly.crypto.service;

import io.provenly.commons.util.EncodingUtils;
import io.provenly.crypto.model.KeyType;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Service for cryptographic signing and verification operations.
 */
@Slf4j
public class SigningService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Sign data with a private key.
     */
    public String sign(byte[] data, byte[] privateKeyBytes, KeyType keyType) {
        return switch (keyType) {
            case RSA -> signWithRsa(data, privateKeyBytes);
            case ED25519 -> signWithEd25519(data, privateKeyBytes);
            case SECP256K1 -> signWithSecp256k1(data, privateKeyBytes);
            case SECP256R1, SECP384R1, SECP521R1 -> signWithEcdsa(data, privateKeyBytes, keyType);
            case BLS12_381 -> throw new UnsupportedOperationException("BLS12-381 not yet implemented");
        };
    }

    /**
     * Sign string data.
     */
    public String sign(String data, byte[] privateKeyBytes, KeyType keyType) {
        return sign(data.getBytes(StandardCharsets.UTF_8), privateKeyBytes, keyType);
    }

    /**
     * Verify a signature.
     */
    public boolean verify(byte[] data, String signature, byte[] publicKeyBytes, KeyType keyType) {
        return switch (keyType) {
            case RSA -> verifyWithRsa(data, signature, publicKeyBytes);
            case ED25519 -> verifyWithEd25519(data, signature, publicKeyBytes);
            case SECP256K1 -> verifyWithSecp256k1(data, signature, publicKeyBytes);
            case SECP256R1, SECP384R1, SECP521R1 -> verifyWithEcdsa(data, signature, publicKeyBytes, keyType);
            case BLS12_381 -> throw new UnsupportedOperationException("BLS12-381 not yet implemented");
        };
    }

    /**
     * Verify string data signature.
     */
    public boolean verify(String data, String signature, byte[] publicKeyBytes, KeyType keyType) {
        return verify(data.getBytes(StandardCharsets.UTF_8), signature, publicKeyBytes, keyType);
    }

    /**
     * Sign with RSA.
     */
    private String signWithRsa(byte[] data, byte[] privateKeyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(data);

            return EncodingUtils.encodeBase64(signature.sign());
        } catch (Exception e) {
            log.error("Failed to sign with RSA", e);
            throw new RuntimeException("Failed to sign with RSA", e);
        }
    }

    /**
     * Verify RSA signature.
     */
    private boolean verifyWithRsa(byte[] data, String signatureStr, byte[] publicKeyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data);

            return signature.verify(EncodingUtils.decodeBase64(signatureStr));
        } catch (Exception e) {
            log.error("Failed to verify RSA signature", e);
            return false;
        }
    }

    /**
     * Sign with Ed25519.
     */
    private String signWithEd25519(byte[] data, byte[] privateKeyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC");
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            Signature signature = Signature.getInstance("Ed25519", "BC");
            signature.initSign(privateKey);
            signature.update(data);

            return EncodingUtils.encodeBase64(signature.sign());
        } catch (Exception e) {
            log.error("Failed to sign with Ed25519", e);
            throw new RuntimeException("Failed to sign with Ed25519", e);
        }
    }

    /**
     * Verify Ed25519 signature.
     */
    private boolean verifyWithEd25519(byte[] data, String signatureStr, byte[] publicKeyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519", "BC");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            Signature signature = Signature.getInstance("Ed25519", "BC");
            signature.initVerify(publicKey);
            signature.update(data);

            return signature.verify(EncodingUtils.decodeBase64(signatureStr));
        } catch (Exception e) {
            log.error("Failed to verify Ed25519 signature", e);
            return false;
        }
    }

    /**
     * Sign with SECP256K1 using Web3j.
     */
    private String signWithSecp256k1(byte[] data, byte[] privateKeyBytes) {
        try {
            BigInteger privateKeyInt = new BigInteger(1, privateKeyBytes);
            ECKeyPair keyPair = ECKeyPair.create(privateKeyInt);

            // Hash the data first
            byte[] hash = hashSha256(data);
            Sign.SignatureData signatureData = Sign.signMessage(hash, keyPair, false);

            // Combine r, s, v into single signature
            byte[] signature = new byte[65];
            System.arraycopy(signatureData.getR(), 0, signature, 0, 32);
            System.arraycopy(signatureData.getS(), 0, signature, 32, 32);
            signature[64] = signatureData.getV()[0];

            return EncodingUtils.encodeBase64(signature);
        } catch (Exception e) {
            log.error("Failed to sign with SECP256K1", e);
            throw new RuntimeException("Failed to sign with SECP256K1", e);
        }
    }

    /**
     * Verify SECP256K1 signature.
     */
    private boolean verifyWithSecp256k1(byte[] data, String signatureStr, byte[] publicKeyBytes) {
        try {
            byte[] signatureBytes = EncodingUtils.decodeBase64(signatureStr);
            byte[] hash = hashSha256(data);

            // Extract r, s, v from signature
            byte[] r = new byte[32];
            byte[] s = new byte[32];
            System.arraycopy(signatureBytes, 0, r, 0, 32);
            System.arraycopy(signatureBytes, 32, s, 0, 32);
            byte v = signatureBytes[64];

            Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);
            BigInteger publicKeyInt = new BigInteger(1, publicKeyBytes);

            // Recover public key from signature and verify
            BigInteger recoveredKey = Sign.signedMessageHashToKey(hash, signatureData);
            return recoveredKey.equals(publicKeyInt);
        } catch (Exception e) {
            log.error("Failed to verify SECP256K1 signature", e);
            return false;
        }
    }

    /**
     * Sign with ECDSA (SECP256R1, SECP384R1, SECP521R1).
     */
    private String signWithEcdsa(byte[] data, byte[] privateKeyBytes, KeyType keyType) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            String algorithm = switch (keyType) {
                case SECP256R1 -> "SHA256withECDSA";
                case SECP384R1 -> "SHA384withECDSA";
                case SECP521R1 -> "SHA512withECDSA";
                default -> throw new IllegalArgumentException("Unsupported key type: " + keyType);
            };

            Signature signature = Signature.getInstance(algorithm, "BC");
            signature.initSign(privateKey);
            signature.update(data);

            return EncodingUtils.encodeBase64(signature.sign());
        } catch (Exception e) {
            log.error("Failed to sign with ECDSA", e);
            throw new RuntimeException("Failed to sign with ECDSA", e);
        }
    }

    /**
     * Verify ECDSA signature.
     */
    private boolean verifyWithEcdsa(byte[] data, String signatureStr, byte[] publicKeyBytes, KeyType keyType) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            String algorithm = switch (keyType) {
                case SECP256R1 -> "SHA256withECDSA";
                case SECP384R1 -> "SHA384withECDSA";
                case SECP521R1 -> "SHA512withECDSA";
                default -> throw new IllegalArgumentException("Unsupported key type: " + keyType);
            };

            Signature signature = Signature.getInstance(algorithm, "BC");
            signature.initVerify(publicKey);
            signature.update(data);

            return signature.verify(EncodingUtils.decodeBase64(signatureStr));
        } catch (Exception e) {
            log.error("Failed to verify ECDSA signature", e);
            return false;
        }
    }

    /**
     * Hash data with SHA-256.
     */
    private byte[] hashSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}


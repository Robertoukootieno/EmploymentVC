package io.provenly.cryptocore.service;

import io.provenly.cryptocore.model.CryptoSuite;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Core hashing and MAC operations shared by security-sensitive modules.
 */
public class CryptoCoreService {

    public byte[] hash(byte[] data, CryptoSuite cryptoSuite) {
        try {
            String algorithm = switch (cryptoSuite) {
                case SHA256 -> "SHA-256";
                case SHA512 -> "SHA-512";
                case HMAC_SHA256 -> throw new IllegalArgumentException("Use hmacSha256 for HMAC operations");
            };
            return MessageDigest.getInstance(algorithm).digest(data);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to hash data", exception);
        }
    }

    public String hashHex(String data, CryptoSuite cryptoSuite) {
        return toHex(hash(data.getBytes(StandardCharsets.UTF_8), cryptoSuite));
    }

    public byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to compute HMAC-SHA256", exception);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte currentByte : bytes) {
            builder.append(String.format("%02x", currentByte));
        }
        return builder.toString();
    }
}

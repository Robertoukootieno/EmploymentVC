package io.provenly.crypto.service;

import io.provenly.commons.util.EncodingUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.Security;

/**
 * Service for encryption and decryption operations.
 * Supports AES-GCM for symmetric encryption.
 */
@Slf4j
public class EncryptionService {

    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int AES_KEY_SIZE = 256;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Generate a new AES-256 key.
     */
    public byte[] generateAesKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(AES_KEY_SIZE, new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            return secretKey.getEncoded();
        } catch (Exception e) {
            log.error("Failed to generate AES key", e);
            throw new RuntimeException("Failed to generate AES key", e);
        }
    }

    /**
     * Encrypt data with AES-GCM.
     * Returns Base64-encoded: IV (12 bytes) + encrypted data + auth tag.
     */
    public String encryptAesGcm(byte[] data, byte[] keyBytes) {
        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // Create cipher
            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

            // Encrypt
            byte[] encrypted = cipher.doFinal(data);

            // Combine IV + encrypted data
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return EncodingUtils.encodeBase64(combined);
        } catch (Exception e) {
            log.error("Failed to encrypt with AES-GCM", e);
            throw new RuntimeException("Failed to encrypt with AES-GCM", e);
        }
    }

    /**
     * Encrypt string data with AES-GCM.
     */
    public String encryptAesGcm(String data, byte[] keyBytes) {
        return encryptAesGcm(data.getBytes(), keyBytes);
    }

    /**
     * Decrypt data with AES-GCM.
     * Expects Base64-encoded: IV (12 bytes) + encrypted data + auth tag.
     */
    public byte[] decryptAesGcm(String encryptedData, byte[] keyBytes) {
        try {
            // Decode Base64
            byte[] combined = EncodingUtils.decodeBase64(encryptedData);

            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            // Create cipher
            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            // Decrypt
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            log.error("Failed to decrypt with AES-GCM", e);
            throw new RuntimeException("Failed to decrypt with AES-GCM", e);
        }
    }

    /**
     * Decrypt to string.
     */
    public String decryptAesGcmToString(String encryptedData, byte[] keyBytes) {
        return new String(decryptAesGcm(encryptedData, keyBytes));
    }

    /**
     * Encrypt data with a password (derives key from password).
     */
    public String encryptWithPassword(byte[] data, String password) {
        byte[] key = deriveKeyFromPassword(password);
        return encryptAesGcm(data, key);
    }

    /**
     * Decrypt data with a password.
     */
    public byte[] decryptWithPassword(String encryptedData, String password) {
        byte[] key = deriveKeyFromPassword(password);
        return decryptAesGcm(encryptedData, key);
    }

    /**
     * Derive a 256-bit key from a password using SHA-256.
     * Note: In production, use PBKDF2 or Argon2 for better security.
     */
    private byte[] deriveKeyFromPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return digest.digest(password.getBytes());
        } catch (Exception e) {
            log.error("Failed to derive key from password", e);
            throw new RuntimeException("Failed to derive key from password", e);
        }
    }
}


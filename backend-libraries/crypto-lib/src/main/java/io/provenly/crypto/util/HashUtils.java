package io.provenly.crypto.util;

import io.provenly.commons.util.EncodingUtils;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for cryptographic hashing operations.
 */
@Slf4j
public final class HashUtils {

    private HashUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Hash data with SHA-256.
     */
    public static byte[] sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Hash string with SHA-256.
     */
    public static byte[] sha256(String data) {
        return sha256(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Hash data with SHA-256 and return hex string.
     */
    public static String sha256Hex(byte[] data) {
        return EncodingUtils.encodeHex(sha256(data));
    }

    /**
     * Hash string with SHA-256 and return hex string.
     */
    public static String sha256Hex(String data) {
        return sha256Hex(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Hash data with SHA-512.
     */
    public static byte[] sha512(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 algorithm not available", e);
        }
    }

    /**
     * Hash string with SHA-512.
     */
    public static byte[] sha512(String data) {
        return sha512(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Hash data with SHA-512 and return hex string.
     */
    public static String sha512Hex(byte[] data) {
        return EncodingUtils.encodeHex(sha512(data));
    }

    /**
     * Hash string with SHA-512 and return hex string.
     */
    public static String sha512Hex(String data) {
        return sha512Hex(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Hash data with SHA-384.
     */
    public static byte[] sha384(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-384");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-384 algorithm not available", e);
        }
    }

    /**
     * Hash string with SHA-384.
     */
    public static byte[] sha384(String data) {
        return sha384(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Hash data with SHA-1 (legacy, not recommended for security).
     */
    public static byte[] sha1(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }

    /**
     * Hash string with SHA-1.
     */
    public static byte[] sha1(String data) {
        return sha1(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Hash data with MD5 (legacy, not recommended for security).
     */
    public static byte[] md5(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * Hash string with MD5.
     */
    public static byte[] md5(String data) {
        return md5(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Hash data with MD5 and return hex string.
     */
    public static String md5Hex(byte[] data) {
        return EncodingUtils.encodeHex(md5(data));
    }

    /**
     * Hash string with MD5 and return hex string.
     */
    public static String md5Hex(String data) {
        return md5Hex(data.getBytes(StandardCharsets.UTF_8));
    }
}


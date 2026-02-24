package io.provenly.commons.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;

/**
 * Utility class for encoding and decoding operations.
 * Provides Base64, Hex, and URL encoding utilities.
 */
public final class EncodingUtils {

    private EncodingUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Encode bytes to Base64 string.
     */
    public static String encodeBase64(byte[] data) {
        return Base64.encodeBase64String(data);
    }

    /**
     * Encode string to Base64.
     */
    public static String encodeBase64(String data) {
        return encodeBase64(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encode bytes to Base64 URL-safe string.
     */
    public static String encodeBase64UrlSafe(byte[] data) {
        return Base64.encodeBase64URLSafeString(data);
    }

    /**
     * Encode string to Base64 URL-safe.
     */
    public static String encodeBase64UrlSafe(String data) {
        return encodeBase64UrlSafe(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decode Base64 string to bytes.
     */
    public static byte[] decodeBase64(String encoded) {
        return Base64.decodeBase64(encoded);
    }

    /**
     * Decode Base64 string to string.
     */
    public static String decodeBase64ToString(String encoded) {
        return new String(decodeBase64(encoded), StandardCharsets.UTF_8);
    }

    /**
     * Encode bytes to hexadecimal string.
     */
    public static String encodeHex(byte[] data) {
        return Hex.encodeHexString(data);
    }

    /**
     * Encode string to hexadecimal.
     */
    public static String encodeHex(String data) {
        return encodeHex(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decode hexadecimal string to bytes.
     */
    public static byte[] decodeHex(String hex) {
        try {
            return Hex.decodeHex(hex);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid hex string: " + hex, e);
        }
    }

    /**
     * Decode hexadecimal string to string.
     */
    public static String decodeHexToString(String hex) {
        return new String(decodeHex(hex), StandardCharsets.UTF_8);
    }

    /**
     * Check if a string is valid Base64.
     */
    public static boolean isValidBase64(String str) {
        if (ValidationUtils.isBlank(str)) {
            return false;
        }
        try {
            Base64.decodeBase64(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a string is valid hexadecimal.
     */
    public static boolean isValidHex(String str) {
        if (ValidationUtils.isBlank(str)) {
            return false;
        }
        try {
            Hex.decodeHex(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convert hex string to Base64.
     */
    public static String hexToBase64(String hex) {
        return encodeBase64(decodeHex(hex));
    }

    /**
     * Convert Base64 string to hex.
     */
    public static String base64ToHex(String base64) {
        return encodeHex(decodeBase64(base64));
    }
}


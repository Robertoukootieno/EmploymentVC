package io.provenly.commons.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Utility class for common validation operations.
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern DID_PATTERN = Pattern.compile(
            "^did:[a-z0-9]+:[a-zA-Z0-9._%-]*[a-zA-Z0-9]$"
    );

    private static final Pattern ETHEREUM_ADDRESS_PATTERN = Pattern.compile(
            "^0x[a-fA-F0-9]{40}$"
    );

    private ValidationUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Check if a string is null or empty.
     */
    public static boolean isEmpty(String str) {
        return StringUtils.isEmpty(str);
    }

    /**
     * Check if a string is not null and not empty.
     */
    public static boolean isNotEmpty(String str) {
        return StringUtils.isNotEmpty(str);
    }

    /**
     * Check if a string is null, empty, or contains only whitespace.
     */
    public static boolean isBlank(String str) {
        return StringUtils.isBlank(str);
    }

    /**
     * Check if a string is not blank.
     */
    public static boolean isNotBlank(String str) {
        return StringUtils.isNotBlank(str);
    }

    /**
     * Check if a collection is null or empty.
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Check if a collection is not null and not empty.
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    /**
     * Check if a map is null or empty.
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Check if a map is not null and not empty.
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    /**
     * Validate email format.
     */
    public static boolean isValidEmail(String email) {
        return isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate DID format.
     */
    public static boolean isValidDid(String did) {
        return isNotBlank(did) && DID_PATTERN.matcher(did).matches();
    }

    /**
     * Validate Ethereum address format.
     */
    public static boolean isValidEthereumAddress(String address) {
        return isNotBlank(address) && ETHEREUM_ADDRESS_PATTERN.matcher(address).matches();
    }

    /**
     * Validate UUID format.
     */
    public static boolean isValidUuid(String uuid) {
        if (isBlank(uuid)) {
            return false;
        }
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Require that a value is not null.
     */
    public static <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * Require that a string is not blank.
     */
    public static String requireNotBlank(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * Require that a collection is not empty.
     */
    public static <T extends Collection<?>> T requireNotEmpty(T collection, String message) {
        if (isEmpty(collection)) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    /**
     * Require that a condition is true.
     */
    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}


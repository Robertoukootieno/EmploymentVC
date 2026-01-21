package io.provenly.commons.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for date and time operations.
 */
public final class DateTimeUtils {

    public static final DateTimeFormatter ISO_8601_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"));
    
    public static final DateTimeFormatter ISO_DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"));
    
    public static final DateTimeFormatter ISO_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("UTC"));

    private DateTimeUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Get current UTC instant.
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * Get current UTC date.
     */
    public static LocalDate today() {
        return LocalDate.now(ZoneId.of("UTC"));
    }

    /**
     * Format instant to ISO-8601 string.
     */
    public static String formatIso8601(Instant instant) {
        return ISO_8601_FORMATTER.format(instant);
    }

    /**
     * Format instant to ISO date string.
     */
    public static String formatIsoDate(Instant instant) {
        return ISO_DATE_FORMATTER.format(instant);
    }

    /**
     * Parse ISO-8601 string to instant.
     */
    public static Instant parseIso8601(String dateTime) {
        return Instant.parse(dateTime);
    }

    /**
     * Parse ISO date string to LocalDate.
     */
    public static LocalDate parseIsoDate(String date) {
        return LocalDate.parse(date, ISO_DATE_FORMATTER);
    }

    /**
     * Add days to instant.
     */
    public static Instant addDays(Instant instant, long days) {
        return instant.plus(days, ChronoUnit.DAYS);
    }

    /**
     * Add hours to instant.
     */
    public static Instant addHours(Instant instant, long hours) {
        return instant.plus(hours, ChronoUnit.HOURS);
    }

    /**
     * Add minutes to instant.
     */
    public static Instant addMinutes(Instant instant, long minutes) {
        return instant.plus(minutes, ChronoUnit.MINUTES);
    }

    /**
     * Subtract days from instant.
     */
    public static Instant subtractDays(Instant instant, long days) {
        return instant.minus(days, ChronoUnit.DAYS);
    }

    /**
     * Check if instant is in the past.
     */
    public static boolean isPast(Instant instant) {
        return instant.isBefore(Instant.now());
    }

    /**
     * Check if instant is in the future.
     */
    public static boolean isFuture(Instant instant) {
        return instant.isAfter(Instant.now());
    }

    /**
     * Check if instant is expired (before now).
     */
    public static boolean isExpired(Instant expirationTime) {
        return isPast(expirationTime);
    }

    /**
     * Get days between two instants.
     */
    public static long daysBetween(Instant start, Instant end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Get hours between two instants.
     */
    public static long hoursBetween(Instant start, Instant end) {
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * Get minutes between two instants.
     */
    public static long minutesBetween(Instant start, Instant end) {
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * Convert epoch milliseconds to instant.
     */
    public static Instant fromEpochMilli(long epochMilli) {
        return Instant.ofEpochMilli(epochMilli);
    }

    /**
     * Convert instant to epoch milliseconds.
     */
    public static long toEpochMilli(Instant instant) {
        return instant.toEpochMilli();
    }

    /**
     * Get start of day for a given instant.
     */
    public static Instant startOfDay(Instant instant) {
        return instant.truncatedTo(ChronoUnit.DAYS);
    }

    /**
     * Get end of day for a given instant.
     */
    public static Instant endOfDay(Instant instant) {
        return startOfDay(instant).plus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.MILLIS);
    }
}


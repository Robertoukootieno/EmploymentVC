package io.provenly.commons.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard API response wrapper for all REST endpoints.
 * Provides consistent response structure across the platform.
 *
 * @param <T> The type of data being returned
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * Indicates if the request was successful.
     */
    private boolean success;

    /**
     * Human-readable message about the response.
     */
    private String message;

    /**
     * The actual response data.
     */
    private T data;

    /**
     * Error information if the request failed.
     */
    private ErrorDetails error;

    /**
     * Timestamp of the response.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Request path that generated this response.
     */
    private String path;

    /**
     * Create a successful response with data.
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Request completed successfully")
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a successful response with data and custom message.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an error response.
     */
    public static <T> ApiResponse<T> error(String message, String errorCode, int status) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorDetails.builder()
                        .code(errorCode)
                        .status(status)
                        .timestamp(Instant.now())
                        .build())
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an error response with details.
     */
    public static <T> ApiResponse<T> error(ErrorDetails errorDetails) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(errorDetails.getMessage())
                .error(errorDetails)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Error details structure.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        /**
         * Error code for categorization.
         */
        private String code;

        /**
         * Human-readable error message.
         */
        private String message;

        /**
         * HTTP status code.
         */
        private int status;

        /**
         * Unique error ID for tracking.
         */
        private String errorId;

        /**
         * When the error occurred.
         */
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private Instant timestamp;

        /**
         * Additional error details.
         */
        private Object details;

        /**
         * Field-specific validation errors.
         */
        private java.util.Map<String, String> fieldErrors;
    }
}


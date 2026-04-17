package dev.payment.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorDetails error,
        Meta meta,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> success(T data, Meta meta) {
        return new ApiResponse<>(true, data, null, meta, Instant.now());
    }

    public static <T> ApiResponse<T> failure(ErrorDetails error) {
        return new ApiResponse<>(false, null, error, null, Instant.now());
    }
}

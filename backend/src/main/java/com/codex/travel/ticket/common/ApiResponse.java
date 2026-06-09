package com.codex.travel.ticket.common;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "ok", data, Instant.now());
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, "CREATED", "created", data, Instant.now());
    }

    public static <T> ApiResponse<T> failed(String code, String message) {
        return new ApiResponse<>(false, code, message, null, Instant.now());
    }
}

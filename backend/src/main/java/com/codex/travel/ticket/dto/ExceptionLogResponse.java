package com.codex.travel.ticket.dto;

import java.time.Instant;

import com.codex.travel.ticket.entity.ExceptionLog;

public record ExceptionLogResponse(
        Long id,
        String traceId,
        Long tenantId,
        String httpMethod,
        String requestUri,
        String queryString,
        Integer statusCode,
        String errorCode,
        String exceptionType,
        String message,
        String stackTrace,
        String clientIp,
        String userAgent,
        Instant createdAt
) {
    public static ExceptionLogResponse from(ExceptionLog log) {
        return new ExceptionLogResponse(
                log.getId(),
                log.getTraceId(),
                log.getTenantId(),
                log.getHttpMethod(),
                log.getRequestUri(),
                log.getQueryString(),
                log.getStatusCode(),
                log.getErrorCode(),
                log.getExceptionType(),
                log.getMessage(),
                log.getStackTrace(),
                log.getClientIp(),
                log.getUserAgent(),
                log.getCreatedAt());
    }
}

package com.codex.travel.ticket.dto;

import java.time.Instant;

import com.codex.travel.ticket.entity.OperationLog;

public record OperationLogResponse(
        Long id,
        String traceId,
        Long tenantId,
        String module,
        String operation,
        String httpMethod,
        String requestUri,
        String queryString,
        Integer statusCode,
        Boolean success,
        Long elapsedMs,
        String clientIp,
        String userAgent,
        Instant createdAt
) {
    public static OperationLogResponse from(OperationLog log) {
        return new OperationLogResponse(
                log.getId(),
                log.getTraceId(),
                log.getTenantId(),
                log.getModule(),
                log.getOperation(),
                log.getHttpMethod(),
                log.getRequestUri(),
                log.getQueryString(),
                log.getStatusCode(),
                log.getSuccess(),
                log.getElapsedMs(),
                log.getClientIp(),
                log.getUserAgent(),
                log.getCreatedAt());
    }
}

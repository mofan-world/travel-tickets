package com.codex.travel.ticket.config;

import com.codex.travel.ticket.service.OperationLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiRequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiRequestLoggingInterceptor.class);
    private static final String START_TIME_ATTRIBUTE = ApiRequestLoggingInterceptor.class.getName() + ".startTime";

    private final OperationLogService operationLogService;

    public ApiRequestLoggingInterceptor(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        MDC.put("traceId", operationLogService.ensureTraceId(request));
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        long elapsedMillis = startTime instanceof Long startedAt
                ? System.currentTimeMillis() - startedAt
                : -1;

        String query = request.getQueryString();
        String path = query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
        String tenantId = request.getHeader("X-Tenant-Id");
        boolean success = ex == null && response.getStatus() < 400;
        if (ex == null) {
            log.info("HTTP {} {} status={} elapsed={}ms tenant={}",
                    request.getMethod(),
                    path,
                    response.getStatus(),
                    elapsedMillis,
                    tenantId == null ? "-" : tenantId);
        } else {
            log.warn("HTTP {} {} status={} elapsed={}ms tenant={} error={}",
                    request.getMethod(),
                    path,
                    response.getStatus(),
                    elapsedMillis,
                    tenantId == null ? "-" : tenantId,
                    ex.getClass().getSimpleName());
        }

        operationLogService.recordOperation(request, response.getStatus(), elapsedMillis, success);
        MDC.remove("traceId");
    }
}

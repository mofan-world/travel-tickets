package com.codex.travel.ticket.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

import com.codex.travel.ticket.common.PageResult;
import com.codex.travel.ticket.dto.ExceptionLogResponse;
import com.codex.travel.ticket.dto.OperationLogResponse;
import com.codex.travel.ticket.entity.ExceptionLog;
import com.codex.travel.ticket.entity.OperationLog;
import com.codex.travel.ticket.repository.ExceptionLogRepository;
import com.codex.travel.ticket.repository.OperationLogRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OperationLogService {

    public static final String TRACE_ID_ATTRIBUTE = OperationLogService.class.getName() + ".traceId";

    private static final Logger log = LoggerFactory.getLogger(OperationLogService.class);
    private static final int MAX_URI_LENGTH = 512;
    private static final int MAX_USER_AGENT_LENGTH = 512;
    private static final int MAX_EXCEPTION_TYPE_LENGTH = 200;
    private static final int MAX_STACK_TRACE_LENGTH = 12000;

    private final OperationLogRepository operationLogRepository;
    private final ExceptionLogRepository exceptionLogRepository;

    public OperationLogService(
            OperationLogRepository operationLogRepository,
            ExceptionLogRepository exceptionLogRepository) {
        this.operationLogRepository = operationLogRepository;
        this.exceptionLogRepository = exceptionLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOperation(HttpServletRequest request, int statusCode, long elapsedMs, boolean success) {
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setTraceId(traceId(request));
            operationLog.setTenantId(tenantId(request));
            operationLog.setModule(module(request));
            operationLog.setOperation(operation(request));
            operationLog.setHttpMethod(request.getMethod());
            operationLog.setRequestUri(truncate(request.getRequestURI(), MAX_URI_LENGTH));
            operationLog.setQueryString(request.getQueryString());
            operationLog.setStatusCode(statusCode);
            operationLog.setSuccess(success);
            operationLog.setElapsedMs(elapsedMs);
            operationLog.setClientIp(clientIp(request));
            operationLog.setUserAgent(truncate(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH));
            operationLogRepository.save(operationLog);
        } catch (Exception ex) {
            log.warn("failed to persist operation log", ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordException(HttpServletRequest request, int statusCode, String errorCode, Exception exception) {
        try {
            ExceptionLog exceptionLog = new ExceptionLog();
            exceptionLog.setTraceId(traceId(request));
            exceptionLog.setTenantId(tenantId(request));
            exceptionLog.setHttpMethod(request.getMethod());
            exceptionLog.setRequestUri(truncate(request.getRequestURI(), MAX_URI_LENGTH));
            exceptionLog.setQueryString(request.getQueryString());
            exceptionLog.setStatusCode(statusCode);
            exceptionLog.setErrorCode(errorCode);
            exceptionLog.setExceptionType(truncate(exception.getClass().getName(), MAX_EXCEPTION_TYPE_LENGTH));
            exceptionLog.setMessage(exception.getMessage());
            exceptionLog.setStackTrace(truncate(stackTrace(exception), MAX_STACK_TRACE_LENGTH));
            exceptionLog.setClientIp(clientIp(request));
            exceptionLog.setUserAgent(truncate(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH));
            exceptionLogRepository.save(exceptionLog);
        } catch (Exception ex) {
            log.warn("failed to persist exception log", ex);
        }
    }

    @Transactional(readOnly = true)
    public PageResult<OperationLogResponse> listOperationLogs(Long tenantId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OperationLog> logs = tenantId == null
                ? operationLogRepository.findAll(pageRequest)
                : operationLogRepository.findByTenantId(tenantId, pageRequest);
        return new PageResult<>(
                logs.getContent().stream().map(OperationLogResponse::from).toList(),
                logs.getNumber(),
                logs.getSize(),
                logs.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResult<ExceptionLogResponse> listExceptionLogs(Long tenantId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ExceptionLog> logs = tenantId == null
                ? exceptionLogRepository.findAll(pageRequest)
                : exceptionLogRepository.findByTenantId(tenantId, pageRequest);
        return new PageResult<>(
                logs.getContent().stream().map(ExceptionLogResponse::from).toList(),
                logs.getNumber(),
                logs.getSize(),
                logs.getTotalElements());
    }

    public String ensureTraceId(HttpServletRequest request) {
        Object current = request.getAttribute(TRACE_ID_ATTRIBUTE);
        if (current instanceof String traceId && StringUtils.hasText(traceId)) {
            return traceId;
        }
        String traceId = UUID.randomUUID().toString().replace("-", "");
        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        return traceId;
    }

    private String traceId(HttpServletRequest request) {
        return ensureTraceId(request);
    }

    private Long tenantId(HttpServletRequest request) {
        String rawTenantId = request.getHeader("X-Tenant-Id");
        if (!StringUtils.hasText(rawTenantId)) {
            return null;
        }
        try {
            return Long.parseLong(rawTenantId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String module(HttpServletRequest request) {
        String[] segments = request.getRequestURI().split("/");
        if (segments.length >= 4 && "api".equals(segments[1]) && "v1".equals(segments[2])) {
            return segments[3];
        }
        return "unknown";
    }

    private String operation(HttpServletRequest request) {
        return request.getMethod() + " " + request.getRequestURI();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String stackTrace(Exception exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}

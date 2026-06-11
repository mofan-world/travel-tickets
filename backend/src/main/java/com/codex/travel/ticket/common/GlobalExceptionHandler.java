package com.codex.travel.ticket.common;

import java.util.stream.Collectors;

import com.codex.travel.ticket.service.OperationLogService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final OperationLogService operationLogService;

    public GlobalExceptionHandler(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        operationLogService.recordException(request, HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", ex);
        return ResponseEntity.badRequest().body(ApiResponse.failed("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceUnavailable(IllegalStateException ex, HttpServletRequest request) {
        operationLogService.recordException(request, HttpStatus.SERVICE_UNAVAILABLE.value(), "SERVICE_UNAVAILABLE", ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failed("SERVICE_UNAVAILABLE", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        operationLogService.recordException(request, HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", ex);
        return ResponseEntity.badRequest().body(ApiResponse.failed("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled request error", ex);
        operationLogService.recordException(request, HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failed("INTERNAL_ERROR", "internal server error"));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }
}

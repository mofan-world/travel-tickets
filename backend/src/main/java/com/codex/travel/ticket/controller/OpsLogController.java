package com.codex.travel.ticket.controller;

import com.codex.travel.ticket.common.ApiResponse;
import com.codex.travel.ticket.common.PageResult;
import com.codex.travel.ticket.dto.ExceptionLogResponse;
import com.codex.travel.ticket.dto.OperationLogResponse;
import com.codex.travel.ticket.service.OperationLogService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops/logs")
public class OpsLogController {

    private final OperationLogService operationLogService;

    public OpsLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping("/operations")
    public ApiResponse<PageResult<OperationLogResponse>> operationLogs(
            @RequestHeader(name = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(operationLogService.listOperationLogs(tenantId, page, size));
    }

    @GetMapping("/exceptions")
    public ApiResponse<PageResult<ExceptionLogResponse>> exceptionLogs(
            @RequestHeader(name = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(operationLogService.listExceptionLogs(tenantId, page, size));
    }
}

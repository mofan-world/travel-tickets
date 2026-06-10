package com.codex.travel.ticket.controller;

import com.codex.travel.ticket.common.ApiResponse;
import com.codex.travel.ticket.common.PageResult;
import com.codex.travel.ticket.dto.CreateTicketRequest;
import com.codex.travel.ticket.dto.TicketResponse;
import com.codex.travel.ticket.dto.UpdateTicketRequest;
import com.codex.travel.ticket.enums.TicketStatus;
import com.codex.travel.ticket.service.TicketService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ApiResponse<PageResult<TicketResponse>> list(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestParam(name = "status", required = false) TicketStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        return ApiResponse.ok(ticketService.list(tenantId, status, page, size));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponse>> create(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(ticketService.create(tenantId, request)));
    }

    @GetMapping("/{ticketId}")
    public ApiResponse<TicketResponse> get(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @PathVariable(name = "ticketId") Long ticketId) {
        return ApiResponse.ok(ticketService.get(tenantId, ticketId));
    }

    @PutMapping("/{ticketId}")
    public ApiResponse<TicketResponse> update(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @PathVariable(name = "ticketId") Long ticketId,
            @Valid @RequestBody UpdateTicketRequest request) {
        return ApiResponse.ok(ticketService.update(tenantId, ticketId, request));
    }

    @DeleteMapping("/{ticketId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @PathVariable(name = "ticketId") Long ticketId) {
        ticketService.delete(tenantId, ticketId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}

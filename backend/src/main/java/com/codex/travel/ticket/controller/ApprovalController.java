package com.codex.travel.ticket.controller;

import com.codex.travel.ticket.common.ApiResponse;
import com.codex.travel.ticket.dto.ApprovalActionRequest;
import com.codex.travel.ticket.dto.TicketResponse;
import com.codex.travel.ticket.service.TicketService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

    private final TicketService ticketService;

    public ApprovalController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/tickets/{ticketId}/actions")
    public ApiResponse<TicketResponse> action(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @PathVariable(name = "ticketId") Long ticketId,
            @Valid @RequestBody ApprovalActionRequest request) {
        return ApiResponse.ok(ticketService.applyApprovalAction(tenantId, ticketId, request));
    }
}

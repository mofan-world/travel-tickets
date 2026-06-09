package com.codex.travel.ticket.controller;

import com.codex.travel.ticket.common.ApiResponse;
import com.codex.travel.ticket.dto.DashboardSummaryResponse;
import com.codex.travel.ticket.service.TicketService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final TicketService ticketService;

    public ReportController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary(@RequestHeader("X-Tenant-Id") Long tenantId) {
        return ApiResponse.ok(ticketService.dashboardSummary(tenantId));
    }
}

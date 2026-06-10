package com.codex.travel.ticket.controller;

import com.codex.travel.ticket.common.ApiResponse;
import com.codex.travel.ticket.common.PageResult;
import com.codex.travel.ticket.dto.RiskEventResponse;
import com.codex.travel.ticket.service.TicketService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/risk")
public class RiskController {

    private final TicketService ticketService;

    public RiskController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/events")
    public ApiResponse<PageResult<RiskEventResponse>> events(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {
        return ApiResponse.ok(ticketService.listRiskEvents(tenantId, page, size));
    }
}

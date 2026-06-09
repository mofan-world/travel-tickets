package com.codex.travel.ticket.controller;

import java.util.List;

import com.codex.travel.ticket.common.ApiResponse;
import com.codex.travel.ticket.dto.RiskEventResponse;
import com.codex.travel.ticket.service.TicketService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/risk")
public class RiskController {

    private final TicketService ticketService;

    public RiskController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/events")
    public ApiResponse<List<RiskEventResponse>> events(@RequestHeader("X-Tenant-Id") Long tenantId) {
        return ApiResponse.ok(ticketService.listRiskEvents(tenantId));
    }
}

package com.codex.travel.ticket.controller;

import com.codex.travel.ticket.common.ApiResponse;
import com.codex.travel.ticket.common.PageResult;
import com.codex.travel.ticket.dto.RiskEventResponse;
import com.codex.travel.ticket.enums.TicketStatus;
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
            @RequestParam(name = "status", required = false) TicketStatus status,
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "travelType", required = false) String travelType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(ticketService.listRiskEvents(tenantId, status, keyword, city, travelType, page, size));
    }
}

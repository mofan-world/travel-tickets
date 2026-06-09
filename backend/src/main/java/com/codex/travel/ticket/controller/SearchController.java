package com.codex.travel.ticket.controller;

import com.codex.travel.ticket.common.ApiResponse;
import com.codex.travel.ticket.common.PageResult;
import com.codex.travel.ticket.dto.TicketSearchResponse;
import com.codex.travel.ticket.service.TicketSearchService;
import com.codex.travel.ticket.service.TicketService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final TicketSearchService ticketSearchService;
    private final TicketService ticketService;

    public SearchController(TicketSearchService ticketSearchService, TicketService ticketService) {
        this.ticketSearchService = ticketSearchService;
        this.ticketService = ticketService;
    }

    @GetMapping("/tickets")
    public ApiResponse<PageResult<TicketSearchResponse>> searchTickets(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(ticketSearchService.search(tenantId, keyword, page, size));
    }

    @PostMapping("/tickets/reindex")
    public ApiResponse<Long> reindexTickets(@RequestHeader("X-Tenant-Id") Long tenantId) {
        return ApiResponse.ok(ticketService.reindexTenant(tenantId));
    }
}

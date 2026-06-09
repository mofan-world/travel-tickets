package com.codex.travel.ticket.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long ticketCount,
        BigDecimal pendingAmount,
        double riskRate,
        double averageProcessHours
) {
}

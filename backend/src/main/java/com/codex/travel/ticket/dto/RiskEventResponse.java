package com.codex.travel.ticket.dto;

import java.time.Instant;

import com.codex.travel.ticket.enums.RiskLevel;

public record RiskEventResponse(
        Long ticketId,
        String ticketNo,
        String route,
        String carrierNo,
        RiskLevel riskLevel,
        String message,
        Instant createdAt
) {
}

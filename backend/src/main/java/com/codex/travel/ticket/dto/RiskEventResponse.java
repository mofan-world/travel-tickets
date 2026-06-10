package com.codex.travel.ticket.dto;

import java.time.Instant;

import com.codex.travel.ticket.enums.RiskLevel;

public record RiskEventResponse(
        Long ticketId,
        String ticketNo,
        String employeeName,
        String department,
        String route,
        String carrierNo,
        RiskLevel riskLevel,
        String attachmentStatus,
        String message,
        Instant createdAt
) {
}

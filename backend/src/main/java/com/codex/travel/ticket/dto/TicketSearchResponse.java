package com.codex.travel.ticket.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.codex.travel.ticket.document.TravelTicketDocument;
import com.codex.travel.ticket.enums.RiskLevel;
import com.codex.travel.ticket.enums.TicketStatus;

public record TicketSearchResponse(
        Long ticketId,
        Long employeeId,
        String ticketNo,
        String route,
        String departureCity,
        String arrivalCity,
        String carrierNo,
        BigDecimal amount,
        TicketStatus status,
        RiskLevel riskLevel,
        Instant createdAt
) {
    public static TicketSearchResponse from(TravelTicketDocument document) {
        return new TicketSearchResponse(
                document.getTicketId(),
                document.getEmployeeId(),
                document.getTicketNo(),
                document.getRoute(),
                document.getDepartureCity(),
                document.getArrivalCity(),
                document.getCarrierNo(),
                document.getAmount(),
                document.getStatus(),
                document.getRiskLevel(),
                document.getCreatedAt());
    }
}

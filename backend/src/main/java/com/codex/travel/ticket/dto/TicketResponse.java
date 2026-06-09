package com.codex.travel.ticket.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.codex.travel.ticket.entity.TravelTicket;
import com.codex.travel.ticket.enums.RiskLevel;
import com.codex.travel.ticket.enums.TicketStatus;

public record TicketResponse(
        Long id,
        Long tenantId,
        Long employeeId,
        String ticketNo,
        String route,
        String departureCity,
        String arrivalCity,
        String carrierNo,
        BigDecimal amount,
        String currency,
        TicketStatus status,
        RiskLevel riskLevel,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketResponse from(TravelTicket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTenantId(),
                ticket.getEmployeeId(),
                ticket.getTicketNo(),
                ticket.getDepartureCity() + " -> " + ticket.getArrivalCity(),
                ticket.getDepartureCity(),
                ticket.getArrivalCity(),
                ticket.getCarrierNo(),
                ticket.getAmount(),
                ticket.getCurrency(),
                ticket.getStatus(),
                ticket.getRiskLevel(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}

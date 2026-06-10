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
        String employeeName,
        String department,
        String ticketNo,
        String travelType,
        String route,
        String departureCity,
        String arrivalCity,
        String carrierNo,
        String seatClass,
        String tripPurpose,
        String attachmentStatus,
        Instant departAt,
        Instant arriveAt,
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
                ticket.getEmployeeName(),
                ticket.getDepartment(),
                ticket.getTicketNo(),
                ticket.getTravelType(),
                ticket.getDepartureCity() + " -> " + ticket.getArrivalCity(),
                ticket.getDepartureCity(),
                ticket.getArrivalCity(),
                ticket.getCarrierNo(),
                ticket.getSeatClass(),
                ticket.getTripPurpose(),
                ticket.getAttachmentStatus(),
                ticket.getDepartAt(),
                ticket.getArriveAt(),
                ticket.getAmount(),
                ticket.getCurrency(),
                ticket.getStatus(),
                ticket.getRiskLevel(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}

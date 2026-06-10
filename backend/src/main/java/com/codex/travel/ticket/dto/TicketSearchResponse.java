package com.codex.travel.ticket.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.codex.travel.ticket.document.TravelTicketDocument;
import com.codex.travel.ticket.enums.RiskLevel;
import com.codex.travel.ticket.enums.TicketStatus;

public record TicketSearchResponse(
        Long ticketId,
        Long employeeId,
        String employeeName,
        String department,
        String ticketNo,
        String travelType,
        String route,
        String departureCity,
        String arrivalCity,
        String carrierNo,
        String tripPurpose,
        String attachmentStatus,
        BigDecimal amount,
        TicketStatus status,
        RiskLevel riskLevel,
        Instant createdAt
) {
    public static TicketSearchResponse from(TravelTicketDocument document) {
        return new TicketSearchResponse(
                document.getTicketId(),
                document.getEmployeeId(),
                document.getEmployeeName(),
                document.getDepartment(),
                document.getTicketNo(),
                document.getTravelType(),
                document.getRoute(),
                document.getDepartureCity(),
                document.getArrivalCity(),
                document.getCarrierNo(),
                document.getTripPurpose(),
                document.getAttachmentStatus(),
                document.getAmount(),
                document.getStatus(),
                document.getRiskLevel(),
                document.getCreatedAt());
    }
}

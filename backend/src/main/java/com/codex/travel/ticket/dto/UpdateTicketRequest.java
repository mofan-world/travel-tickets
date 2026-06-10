package com.codex.travel.ticket.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.codex.travel.ticket.enums.TicketStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketRequest(
        @NotNull Long employeeId,
        @NotBlank String employeeName,
        @NotBlank String department,
        @NotBlank String ticketNo,
        String externalSource,
        String externalTicketId,
        String travelType,
        @NotBlank String departureCity,
        @NotBlank String arrivalCity,
        @NotBlank String carrierNo,
        String seatClass,
        @NotBlank String tripPurpose,
        String attachmentStatus,
        Instant departAt,
        Instant arriveAt,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String currency,
        TicketStatus status
) {
}

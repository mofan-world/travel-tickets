package com.codex.travel.ticket.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTicketRequest(
        @NotNull Long employeeId,
        @NotBlank String ticketNo,
        String externalSource,
        String externalTicketId,
        String travelType,
        @NotBlank String departureCity,
        @NotBlank String arrivalCity,
        @NotBlank String carrierNo,
        String seatClass,
        Instant departAt,
        Instant arriveAt,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String currency
) {
}

package com.codex.travel.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record ApprovalActionRequest(
        @NotBlank String action,
        String comment
) {
}

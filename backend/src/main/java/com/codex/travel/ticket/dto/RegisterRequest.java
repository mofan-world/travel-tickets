package com.codex.travel.ticket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank String company,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password
) {
}

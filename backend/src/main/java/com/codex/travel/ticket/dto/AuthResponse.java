package com.codex.travel.ticket.dto;

import com.codex.travel.ticket.entity.AppUser;

public record AuthResponse(
        Long userId,
        Long tenantId,
        String name,
        String company,
        String email
) {
    public static AuthResponse from(AppUser user) {
        return new AuthResponse(
                user.getId(),
                user.getTenantId(),
                user.getName(),
                user.getCompany(),
                user.getEmail());
    }
}

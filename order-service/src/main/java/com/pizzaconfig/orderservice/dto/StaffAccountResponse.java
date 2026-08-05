package com.pizzaconfig.orderservice.dto;

import com.pizzaconfig.orderservice.domain.StaffAccount;
import com.pizzaconfig.orderservice.domain.StaffRole;

import java.time.Instant;
import java.util.UUID;

public record StaffAccountResponse(
        UUID id,
        String email,
        String fullName,
        StaffRole role,
        boolean mustChangePassword,
        Instant createdAt
) {
    public static StaffAccountResponse from(StaffAccount account) {
        return new StaffAccountResponse(
                account.getId(), account.getEmail(), account.getFullName(),
                account.getRole(), account.isMustChangePassword(), account.getCreatedAt());
    }
}

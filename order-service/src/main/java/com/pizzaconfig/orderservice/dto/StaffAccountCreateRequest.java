package com.pizzaconfig.orderservice.dto;

import com.pizzaconfig.orderservice.domain.StaffRole;

public record StaffAccountCreateRequest(String email, String fullName, StaffRole role) {
}

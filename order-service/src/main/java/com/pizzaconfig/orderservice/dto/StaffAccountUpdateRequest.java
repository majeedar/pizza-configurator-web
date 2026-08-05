package com.pizzaconfig.orderservice.dto;

import com.pizzaconfig.orderservice.domain.StaffRole;

public record StaffAccountUpdateRequest(String fullName, StaffRole role) {
}

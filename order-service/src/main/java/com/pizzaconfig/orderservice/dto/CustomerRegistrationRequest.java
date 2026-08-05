package com.pizzaconfig.orderservice.dto;

public record CustomerRegistrationRequest(
        String email,
        String password,
        String fullName,
        String phoneNumber
) {
}

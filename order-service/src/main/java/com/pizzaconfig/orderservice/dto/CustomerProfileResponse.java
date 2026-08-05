package com.pizzaconfig.orderservice.dto;

import com.pizzaconfig.orderservice.domain.Customer;

import java.util.UUID;

public record CustomerProfileResponse(
        UUID id,
        String email,
        String fullName,
        String phoneNumber
) {
    public static CustomerProfileResponse from(Customer customer) {
        return new CustomerProfileResponse(customer.getId(), customer.getEmail(), customer.getFullName(), customer.getPhoneNumber());
    }
}

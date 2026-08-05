package com.pizzaconfig.orderservice.dto;

// temporaryPassword is only ever populated here, at creation time — it is never
// stored in plaintext and never returned by any other endpoint.
public record StaffAccountCreateResponse(StaffAccountResponse account, String temporaryPassword) {
}

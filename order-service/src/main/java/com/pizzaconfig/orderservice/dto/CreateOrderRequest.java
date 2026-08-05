package com.pizzaconfig.orderservice.dto;

import java.util.List;

public record CreateOrderRequest(
        List<CreateOrderItemRequest> items,
        String customNotes,
        String phoneNumber
) {
}

package com.pizzaconfig.orderservice.dto;

import com.pizzaconfig.orderservice.domain.Modifications;

import java.math.BigDecimal;

public record CreateOrderItemRequest(
        String basePizzaId,
        String chosenSize,
        String chosenDough,
        Modifications modifications,
        BigDecimal subtotal
) {
}

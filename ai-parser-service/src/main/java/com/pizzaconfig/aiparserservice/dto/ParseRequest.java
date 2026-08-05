package com.pizzaconfig.aiparserservice.dto;

public record ParseRequest(
        String basePizzaId,
        String size,
        String dough,
        String comment
) {
}

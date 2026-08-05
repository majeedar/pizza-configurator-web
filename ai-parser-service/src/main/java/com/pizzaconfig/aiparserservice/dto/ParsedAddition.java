package com.pizzaconfig.aiparserservice.dto;

public record ParsedAddition(
        String ingredientId,
        String type,
        int quantity
) {
}

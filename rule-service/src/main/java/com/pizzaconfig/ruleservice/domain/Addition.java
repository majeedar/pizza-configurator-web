package com.pizzaconfig.ruleservice.domain;

public record Addition(
        String ingredientId,
        String type,
        int quantity
) {
}

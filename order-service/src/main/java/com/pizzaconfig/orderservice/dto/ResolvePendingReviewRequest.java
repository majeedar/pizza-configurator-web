package com.pizzaconfig.orderservice.dto;

import java.util.List;

public record ResolvePendingReviewRequest(
        String basePizzaId,
        String size,
        String dough,
        List<AdditionInput> additions,
        List<String> removals
) {
    public record AdditionInput(String ingredientId, String type, int quantity) {
    }
}

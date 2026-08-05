package com.pizzaconfig.commoncontracts.event;

import java.util.List;

/** Structural view of an order item — deliberately excludes price (see CLAUDE.md §5: the KDS must never show price). */
public record OrderItemSummary(
        String basePizzaId,
        String chosenSize,
        String chosenDough,
        List<AdditionSummary> additions,
        List<String> removals
) {
}

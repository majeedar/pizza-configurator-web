package com.pizzaconfig.kitchenservice.domain;

import java.util.List;

public record TicketItem(
        String basePizzaId,
        String chosenSize,
        String chosenDough,
        List<TicketAddition> additions,
        List<String> removals
) {
}

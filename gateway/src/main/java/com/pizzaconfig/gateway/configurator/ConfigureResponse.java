package com.pizzaconfig.gateway.configurator;

import java.util.List;
import java.util.Map;

public record ConfigureResponse(
        String basePizzaId,
        String size,
        String dough,
        List<AdditionDto> additions,
        List<String> removals,
        boolean ambiguous,
        String kitchenNote,
        Map<String, Object> validation
) {
}

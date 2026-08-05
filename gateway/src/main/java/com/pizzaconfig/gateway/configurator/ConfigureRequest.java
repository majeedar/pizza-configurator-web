package com.pizzaconfig.gateway.configurator;

import java.util.List;

public record ConfigureRequest(
        String basePizzaId,
        String size,
        String dough,
        List<AdditionDto> additions,
        List<String> removals,
        String comment
) {
}

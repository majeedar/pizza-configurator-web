package com.pizzaconfig.gateway.configurator;

import java.util.List;

public record ParsedProposal(
        String basePizzaId,
        String size,
        String dough,
        List<AdditionDto> additions,
        List<String> removals,
        boolean ambiguous
) {
}

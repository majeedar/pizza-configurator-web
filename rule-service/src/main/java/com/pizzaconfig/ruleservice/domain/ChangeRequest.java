package com.pizzaconfig.ruleservice.domain;

import java.util.List;

public record ChangeRequest(
        String basePizzaId,
        String size,
        String dough,
        List<Addition> additions,
        List<String> removals,
        ChangeSource source,
        boolean ambiguous
) {
    public ChangeRequest {
        additions = additions == null ? List.of() : additions;
        removals = removals == null ? List.of() : removals;
    }
}

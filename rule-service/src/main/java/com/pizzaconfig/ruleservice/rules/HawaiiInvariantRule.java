package com.pizzaconfig.ruleservice.rules;

import com.pizzaconfig.ruleservice.domain.Addition;
import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.FailedRule;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class HawaiiInvariantRule implements Rule {

    private static final String HAWAII = "hawaii";
    private static final String PINEAPPLE = "pineapple";
    private static final int DEFAULT_PINEAPPLE_QUANTITY = 1;

    @Override
    public Optional<FailedRule> check(ChangeRequest request) {
        if (!HAWAII.equalsIgnoreCase(request.basePizzaId())) {
            return Optional.empty();
        }

        boolean exceedsDefault = request.additions().stream()
                .filter(a -> PINEAPPLE.equalsIgnoreCase(a.ingredientId()))
                .map(Addition::quantity)
                .anyMatch(qty -> qty > DEFAULT_PINEAPPLE_QUANTITY);

        if (exceedsDefault) {
            return Optional.of(new FailedRule(
                    "HAWAII_INVARIANT",
                    "Ananas kann bei Hawaii nur entfernt, nicht über die Standardmenge hinaus hinzugefügt werden."
            ));
        }
        return Optional.empty();
    }
}

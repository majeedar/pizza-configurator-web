package com.pizzaconfig.ruleservice.rules;

import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.FailedRule;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BinarySeparationRule implements Rule {

    private static final String ANCHOVIES = "anchovies";
    private static final String VEGAN_CHEESE = "vegan-cheese";

    @Override
    public Optional<FailedRule> check(ChangeRequest request) {
        boolean hasAnchovies = request.additions().stream()
                .anyMatch(a -> ANCHOVIES.equalsIgnoreCase(a.ingredientId()));
        boolean hasVeganCheese = request.additions().stream()
                .anyMatch(a -> VEGAN_CHEESE.equalsIgnoreCase(a.ingredientId()));

        if (hasAnchovies && hasVeganCheese) {
            return Optional.of(new FailedRule(
                    "BINARY_SEPARATION",
                    "Sardellen und veganer Käse-Ersatz schließen sich gegenseitig aus."
            ));
        }
        return Optional.empty();
    }
}

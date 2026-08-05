package com.pizzaconfig.ruleservice.rules;

import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.FailedRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DoubleCheeseExclusionRule implements Rule {

    @Override
    public Optional<FailedRule> check(ChangeRequest request) {
        List<Boolean> cheeseAdditions = request.additions().stream()
                .filter(a -> "CHEESE".equals(a.type()))
                .map(a -> a.quantity() == 2)
                .toList();

        boolean hasDoubleCheese = cheeseAdditions.stream().anyMatch(isDouble -> isDouble);
        boolean hasOtherCheeseVariety = cheeseAdditions.size() > 1;

        if (hasDoubleCheese && hasOtherCheeseVariety) {
            return Optional.of(new FailedRule(
                    "DOUBLE_CHEESE_EXCLUSION",
                    "Bei doppelter Käseportion sind keine weiteren Extra-Käse-Toppings erlaubt."
            ));
        }
        return Optional.empty();
    }
}

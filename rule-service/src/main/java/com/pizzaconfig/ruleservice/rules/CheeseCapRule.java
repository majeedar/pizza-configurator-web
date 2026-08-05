package com.pizzaconfig.ruleservice.rules;

import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.FailedRule;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CheeseCapRule implements Rule {

    private final RuleProperties properties;

    public CheeseCapRule(RuleProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<FailedRule> check(ChangeRequest request) {
        Set<String> cheeseVarieties = request.additions().stream()
                .filter(a -> "CHEESE".equals(a.type()))
                .map(a -> a.ingredientId())
                .collect(Collectors.toSet());

        if (cheeseVarieties.size() > properties.getCheeseCap()) {
            return Optional.of(new FailedRule(
                    "CHEESE_CAP",
                    "Maximal " + properties.getCheeseCap() + " zusätzliche Käsesorten pro Pizza erlaubt."
            ));
        }
        return Optional.empty();
    }
}

package com.pizzaconfig.ruleservice.rules;

import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.FailedRule;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GlutenFreeConstraintRule implements Rule {

    private static final String GLUTEN_FREE_DOUGH = "gluten-free";

    private final RuleProperties properties;

    public GlutenFreeConstraintRule(RuleProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<FailedRule> check(ChangeRequest request) {
        if (!GLUTEN_FREE_DOUGH.equalsIgnoreCase(request.dough())) {
            return Optional.empty();
        }

        boolean sizeAllowed = properties.getGlutenFreeSizes().stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(request.size()));

        if (!sizeAllowed) {
            return Optional.of(new FailedRule(
                    "GLUTEN_FREE_SIZE",
                    "Glutenfreier Teig ist nur in den Größen " + properties.getGlutenFreeSizes() + " verfügbar."
            ));
        }
        return Optional.empty();
    }
}

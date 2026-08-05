package com.pizzaconfig.ruleservice.rules;

import com.pizzaconfig.ruleservice.domain.Addition;
import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.FailedRule;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OvenThermalLimitRule implements Rule {

    private final RuleProperties properties;

    public OvenThermalLimitRule(RuleProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<FailedRule> check(ChangeRequest request) {
        int totalQuantity = request.additions().stream()
                .mapToInt(Addition::quantity)
                .sum();

        if (totalQuantity > properties.getOvenThermalLimit()) {
            return Optional.of(new FailedRule(
                    "OVEN_THERMAL_LIMIT",
                    "Die Gesamtmenge an Zusatz-Zutaten darf " + properties.getOvenThermalLimit()
                            + " pro Pizza nicht überschreiten."
            ));
        }
        return Optional.empty();
    }
}

package com.pizzaconfig.ruleservice.rules;

import com.pizzaconfig.ruleservice.domain.Addition;
import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.ChangeSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OvenThermalLimitRuleTest {

    private final RuleProperties properties = new RuleProperties();
    private final OvenThermalLimitRule rule = new OvenThermalLimitRule(properties);

    private ChangeRequest requestWithTotalQuantity(int totalQuantity) {
        return new ChangeRequest(
                "margherita", "M", "classic",
                List.of(new Addition("topping", "TOPPING", totalQuantity)),
                List.of(), ChangeSource.BUTTON, false);
    }

    @Test
    void validCase_underLimit_passes() {
        assertThat(rule.check(requestWithTotalQuantity(5))).isEmpty();
    }

    @Test
    void boundaryCase_atLimit_passes() {
        assertThat(rule.check(requestWithTotalQuantity(10))).isEmpty();
    }

    @Test
    void violationCase_overLimit_fails() {
        assertThat(rule.check(requestWithTotalQuantity(11)))
                .isPresent()
                .get()
                .satisfies(failure -> assertThat(failure.ruleId()).isEqualTo("OVEN_THERMAL_LIMIT"));
    }
}

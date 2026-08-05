package com.pizzaconfig.ruleservice.rules;

import com.pizzaconfig.ruleservice.domain.Addition;
import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.ChangeSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CheeseCapRuleTest {

    private final RuleProperties properties = new RuleProperties();
    private final CheeseCapRule rule = new CheeseCapRule(properties);

    private ChangeRequest requestWithCheeses(String... cheeseIds) {
        List<Addition> additions = List.of(cheeseIds).stream()
                .map(id -> new Addition(id, "CHEESE", 1))
                .toList();
        return new ChangeRequest("margherita", "M", "classic", additions, List.of(), ChangeSource.BUTTON, false);
    }

    @Test
    void validCase_underCap_passes() {
        assertThat(rule.check(requestWithCheeses("mozzarella"))).isEmpty();
    }

    @Test
    void boundaryCase_atCap_passes() {
        assertThat(rule.check(requestWithCheeses("mozzarella", "gorgonzola"))).isEmpty();
    }

    @Test
    void violationCase_overCap_fails() {
        assertThat(rule.check(requestWithCheeses("mozzarella", "gorgonzola", "parmesan")))
                .isPresent()
                .get()
                .satisfies(failure -> assertThat(failure.ruleId()).isEqualTo("CHEESE_CAP"));
    }
}

package com.pizzaconfig.ruleservice.rules;

import com.pizzaconfig.ruleservice.domain.Addition;
import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.ChangeSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DoubleCheeseExclusionRuleTest {

    private final DoubleCheeseExclusionRule rule = new DoubleCheeseExclusionRule();

    @Test
    void validCase_singleDoubledCheese_passes() {
        ChangeRequest request = new ChangeRequest(
                "margherita", "M", "classic",
                List.of(new Addition("mozzarella", "CHEESE", 2)),
                List.of(), ChangeSource.BUTTON, false);

        assertThat(rule.check(request)).isEmpty();
    }

    @Test
    void boundaryCase_twoCheesesNoneDoubled_passes() {
        ChangeRequest request = new ChangeRequest(
                "margherita", "M", "classic",
                List.of(new Addition("mozzarella", "CHEESE", 1), new Addition("gorgonzola", "CHEESE", 1)),
                List.of(), ChangeSource.BUTTON, false);

        assertThat(rule.check(request)).isEmpty();
    }

    @Test
    void violationCase_doubledCheesePlusAnotherVariety_fails() {
        ChangeRequest request = new ChangeRequest(
                "margherita", "M", "classic",
                List.of(new Addition("mozzarella", "CHEESE", 2), new Addition("gorgonzola", "CHEESE", 1)),
                List.of(), ChangeSource.BUTTON, false);

        assertThat(rule.check(request))
                .isPresent()
                .get()
                .satisfies(failure -> assertThat(failure.ruleId()).isEqualTo("DOUBLE_CHEESE_EXCLUSION"));
    }
}

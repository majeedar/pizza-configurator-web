package com.pizzaconfig.ruleservice.rules;

import com.pizzaconfig.ruleservice.domain.Addition;
import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.ChangeSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BinarySeparationRuleTest {

    private final BinarySeparationRule rule = new BinarySeparationRule();

    @Test
    void validCase_anchoviesOnly_passes() {
        ChangeRequest request = new ChangeRequest(
                "margherita", "M", "classic",
                List.of(new Addition("anchovies", "TOPPING", 1)),
                List.of(), ChangeSource.BUTTON, false);

        assertThat(rule.check(request)).isEmpty();
    }

    @Test
    void boundaryCase_veganCheeseOnly_passes() {
        ChangeRequest request = new ChangeRequest(
                "margherita", "M", "classic",
                List.of(new Addition("vegan-cheese", "CHEESE", 1)),
                List.of(), ChangeSource.BUTTON, false);

        assertThat(rule.check(request)).isEmpty();
    }

    @Test
    void violationCase_bothAnchoviesAndVeganCheese_fails() {
        ChangeRequest request = new ChangeRequest(
                "margherita", "M", "classic",
                List.of(new Addition("anchovies", "TOPPING", 1), new Addition("vegan-cheese", "CHEESE", 1)),
                List.of(), ChangeSource.BUTTON, false);

        assertThat(rule.check(request))
                .isPresent()
                .get()
                .satisfies(failure -> assertThat(failure.ruleId()).isEqualTo("BINARY_SEPARATION"));
    }
}

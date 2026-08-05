package com.pizzaconfig.ruleservice.rules;

import com.pizzaconfig.ruleservice.domain.Addition;
import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.ChangeSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HawaiiInvariantRuleTest {

    private final HawaiiInvariantRule rule = new HawaiiInvariantRule();

    @Test
    void validCase_nonHawaiiBase_pineappleQuantityIgnored() {
        ChangeRequest request = new ChangeRequest(
                "margherita", "M", "classic",
                List.of(new Addition("pineapple", "TOPPING", 5)),
                List.of(), ChangeSource.BUTTON, false);

        assertThat(rule.check(request)).isEmpty();
    }

    @Test
    void boundaryCase_hawaiiWithDefaultPineappleQuantity_passes() {
        ChangeRequest request = new ChangeRequest(
                "hawaii", "M", "classic",
                List.of(new Addition("pineapple", "TOPPING", 1)),
                List.of(), ChangeSource.BUTTON, false);

        assertThat(rule.check(request)).isEmpty();
    }

    @Test
    void violationCase_hawaiiWithExtraPineapple_fails() {
        ChangeRequest request = new ChangeRequest(
                "hawaii", "M", "classic",
                List.of(new Addition("pineapple", "TOPPING", 2)),
                List.of(), ChangeSource.BUTTON, false);

        assertThat(rule.check(request))
                .isPresent()
                .get()
                .satisfies(failure -> assertThat(failure.ruleId()).isEqualTo("HAWAII_INVARIANT"));
    }
}

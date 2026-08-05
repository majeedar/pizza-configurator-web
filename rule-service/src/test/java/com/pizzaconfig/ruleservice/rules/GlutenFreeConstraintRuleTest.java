package com.pizzaconfig.ruleservice.rules;

import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.ChangeSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlutenFreeConstraintRuleTest {

    private final RuleProperties properties = new RuleProperties();
    private final GlutenFreeConstraintRule rule = new GlutenFreeConstraintRule(properties);

    @Test
    void validCase_nonGlutenFreeDough_sizeIgnored() {
        ChangeRequest request = new ChangeRequest(
                "margherita", "L", "classic", List.of(), List.of(), ChangeSource.BUTTON, false);

        assertThat(rule.check(request)).isEmpty();
    }

    @Test
    void boundaryCase_glutenFreeWithWhitelistedSize_passes() {
        ChangeRequest request = new ChangeRequest(
                "margherita", "M", "gluten-free", List.of(), List.of(), ChangeSource.BUTTON, false);

        assertThat(rule.check(request)).isEmpty();
    }

    @Test
    void violationCase_glutenFreeWithDisallowedSize_fails() {
        ChangeRequest request = new ChangeRequest(
                "margherita", "L", "gluten-free", List.of(), List.of(), ChangeSource.BUTTON, false);

        assertThat(rule.check(request))
                .isPresent()
                .get()
                .satisfies(failure -> assertThat(failure.ruleId()).isEqualTo("GLUTEN_FREE_SIZE"));
    }
}

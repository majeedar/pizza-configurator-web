package com.pizzaconfig.ruleservice.rules;

import com.pizzaconfig.ruleservice.client.CatalogClient;
import com.pizzaconfig.ruleservice.domain.Addition;
import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.ChangeSource;
import com.pizzaconfig.ruleservice.dto.AllowedExtraDto;
import com.pizzaconfig.ruleservice.dto.CatalogPizza;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExtraAllowedForPizzaRuleTest {

    private final CatalogClient catalogClient = mock(CatalogClient.class);
    private final ExtraAllowedForPizzaRule rule = new ExtraAllowedForPizzaRule(catalogClient);

    private void stubCatalog() {
        when(catalogClient.findAll()).thenReturn(List.of(
                new CatalogPizza("margherita", "Margherita", "...", new BigDecimal("8.50"),
                        List.of(new AllowedExtraDto("mozzarella", "Mozzarella", "CHEESE", "PORTION"))),
                new CatalogPizza("salami", "Salami", "...", new BigDecimal("9.00"),
                        List.of(
                                new AllowedExtraDto("mozzarella", "Mozzarella", "CHEESE", "PORTION"),
                                new AllowedExtraDto("pineapple", "Pineapple", "TOPPING", "PIECE")))
        ));
    }

    @Test
    void validCase_allowedExtraForPizza_passes() {
        stubCatalog();
        ChangeRequest request = new ChangeRequest("margherita", "M", "classic",
                List.of(new Addition("mozzarella", "CHEESE", 1)), List.of(), ChangeSource.BUTTON, false);

        assertThat(rule.check(request)).isEmpty();
    }

    @Test
    void boundaryCase_noAdditions_passes() {
        stubCatalog();
        ChangeRequest request = new ChangeRequest("margherita", "M", "classic",
                List.of(), List.of(), ChangeSource.BUTTON, false);

        assertThat(rule.check(request)).isEmpty();
    }

    @Test
    void violationCase_disallowedExtraForPizza_fails() {
        stubCatalog();
        ChangeRequest request = new ChangeRequest("margherita", "M", "classic",
                List.of(new Addition("pineapple", "TOPPING", 1)), List.of(), ChangeSource.BUTTON, false);

        assertThat(rule.check(request))
                .isPresent()
                .get()
                .satisfies(failure -> assertThat(failure.ruleId()).isEqualTo("EXTRA_NOT_ALLOWED_FOR_PIZZA"));
    }
}

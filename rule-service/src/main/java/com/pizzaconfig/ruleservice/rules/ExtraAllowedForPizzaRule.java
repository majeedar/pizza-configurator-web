package com.pizzaconfig.ruleservice.rules;

import com.pizzaconfig.ruleservice.client.CatalogClient;
import com.pizzaconfig.ruleservice.domain.Addition;
import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.FailedRule;
import com.pizzaconfig.ruleservice.dto.AllowedExtraDto;
import com.pizzaconfig.ruleservice.dto.CatalogPizza;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// Catalog-service scopes which extras make sense per base pizza (e.g. Margherita
// doesn't offer pineapple) — this rule is what actually enforces that at validation
// time, so a request that bypasses the UI (or an AI-parsed proposal suggesting an
// ingredient outside that pizza's menu) gets caught rather than silently approved.
@Component
public class ExtraAllowedForPizzaRule implements Rule {

    private final CatalogClient catalogClient;

    public ExtraAllowedForPizzaRule(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    @Override
    public Optional<FailedRule> check(ChangeRequest request) {
        if (request.additions().isEmpty()) {
            return Optional.empty();
        }

        List<CatalogPizza> pizzas = catalogClient.findAll();
        Optional<CatalogPizza> pizza = pizzas.stream()
                .filter(p -> p.id().equalsIgnoreCase(request.basePizzaId()))
                .findFirst();

        if (pizza.isEmpty()) {
            return Optional.empty();
        }

        Set<String> allowedIds = pizza.get().allowedExtras() == null
                ? Set.of()
                : pizza.get().allowedExtras().stream().map(AllowedExtraDto::ingredientId).collect(Collectors.toSet());

        List<String> disallowed = request.additions().stream()
                .map(Addition::ingredientId)
                .filter(id -> !allowedIds.contains(id))
                .distinct()
                .toList();

        if (!disallowed.isEmpty()) {
            return Optional.of(new FailedRule(
                    "EXTRA_NOT_ALLOWED_FOR_PIZZA",
                    "Diese Zutat(en) sind für " + request.basePizzaId() + " nicht verfügbar: "
                            + String.join(", ", disallowed)
            ));
        }
        return Optional.empty();
    }
}

package com.pizzaconfig.ruleservice.service;

import com.pizzaconfig.ruleservice.client.CatalogClient;
import com.pizzaconfig.ruleservice.client.PricingClient;
import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.ChangeSource;
import com.pizzaconfig.ruleservice.domain.FailedRule;
import com.pizzaconfig.ruleservice.domain.RuleOutcome;
import com.pizzaconfig.ruleservice.domain.ValidationResult;
import com.pizzaconfig.ruleservice.rules.Rule;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuleValidationService {

    private final List<Rule> rules;
    private final CatalogClient catalogClient;
    private final PricingClient pricingClient;

    public RuleValidationService(List<Rule> rules, CatalogClient catalogClient, PricingClient pricingClient) {
        this.rules = rules;
        this.catalogClient = catalogClient;
        this.pricingClient = pricingClient;
    }

    public ValidationResult validate(ChangeRequest request) {
        List<FailedRule> failures = rules.stream()
                .map(rule -> rule.check(request))
                .flatMap(java.util.Optional::stream)
                .toList();

        if (failures.isEmpty()) {
            return new ValidationResult(RuleOutcome.APPROVED, List.of(), List.of(), List.of());
        }

        if (request.source() == ChangeSource.FREE_TEXT && request.ambiguous()) {
            return new ValidationResult(RuleOutcome.MANUAL_REVIEW, failures, List.of(), List.of());
        }

        // INVALID: build recommendations by calling catalog-service + pricing-service internally (§5).
        return new ValidationResult(
                RuleOutcome.INVALID,
                failures,
                catalogClient.findAll(),
                pricingClient.findAllPrices()
        );
    }
}

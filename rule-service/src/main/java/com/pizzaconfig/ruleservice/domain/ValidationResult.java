package com.pizzaconfig.ruleservice.domain;

import com.pizzaconfig.ruleservice.dto.CatalogPizza;
import com.pizzaconfig.ruleservice.dto.PriceItemDto;

import java.util.List;

public record ValidationResult(
        RuleOutcome outcome,
        List<FailedRule> failures,
        List<CatalogPizza> recommendations,
        List<PriceItemDto> referencePrices
) {
}

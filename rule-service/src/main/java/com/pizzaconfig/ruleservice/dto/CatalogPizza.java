package com.pizzaconfig.ruleservice.dto;

import java.math.BigDecimal;
import java.util.List;

/** Mirrors catalog-service's Pizza response shape (GET /v1/catalog/pizzas). */
public record CatalogPizza(
        String id,
        String name,
        String description,
        BigDecimal basePrice,
        List<AllowedExtraDto> allowedExtras
) {
}

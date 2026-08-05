package com.pizzaconfig.ruleservice.dto;

/** Mirrors catalog-service's AllowedExtra shape, nested in the Pizza response. */
public record AllowedExtraDto(
        String ingredientId,
        String name,
        String type,
        String unit
) {
}

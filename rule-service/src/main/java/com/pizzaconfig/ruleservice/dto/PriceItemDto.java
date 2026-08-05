package com.pizzaconfig.ruleservice.dto;

import java.math.BigDecimal;

/** Mirrors pricing-service's PriceItem response shape (GET /v1/pricing/prices). */
public record PriceItemDto(
        String itemId,
        String itemType,
        BigDecimal amount
) {
}

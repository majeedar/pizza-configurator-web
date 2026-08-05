package com.pizzaconfig.orderservice.dto;

import com.pizzaconfig.orderservice.domain.Modifications;

public record CreatePendingReviewRequest(
        String basePizzaId,
        String size,
        String dough,
        Modifications modifications,
        String rawComment,
        String phoneNumber
) {
}

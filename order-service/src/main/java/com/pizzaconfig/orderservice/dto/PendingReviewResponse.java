package com.pizzaconfig.orderservice.dto;

import com.pizzaconfig.orderservice.domain.Modifications;
import com.pizzaconfig.orderservice.domain.PendingReview;
import com.pizzaconfig.orderservice.domain.PendingReviewStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PendingReviewResponse(
        UUID id,
        PendingReviewStatus status,
        String basePizzaId,
        String size,
        String dough,
        Modifications modifications,
        String rawComment,
        String resolvedBasePizzaId,
        String resolvedSize,
        String resolvedDough,
        Modifications resolvedModifications,
        BigDecimal resolvedTotalPrice,
        Instant createdAt,
        Instant resolvedAt
) {
    public static PendingReviewResponse from(PendingReview review) {
        return new PendingReviewResponse(
                review.getId(),
                review.getStatus(),
                review.getBasePizzaId(),
                review.getChosenSize(),
                review.getChosenDough(),
                review.getModifications(),
                review.getRawComment(),
                review.getResolvedBasePizzaId(),
                review.getResolvedSize(),
                review.getResolvedDough(),
                review.getResolvedModifications(),
                review.getResolvedTotalPrice(),
                review.getCreatedAt(),
                review.getResolvedAt()
        );
    }
}

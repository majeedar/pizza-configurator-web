package com.pizzaconfig.aiparserservice.dto;

import java.util.List;

/**
 * The structured mutation proposal handed back to the caller (gateway/frontend), which
 * then forwards it to rule-service for validation — ai-parser-service has no business-rule
 * knowledge itself and never decides APPROVED/INVALID/MANUAL_REVIEW.
 */
public record ParsedProposal(
        String basePizzaId,
        String size,
        String dough,
        List<ParsedAddition> additions,
        List<String> removals,
        boolean ambiguous
) {
}

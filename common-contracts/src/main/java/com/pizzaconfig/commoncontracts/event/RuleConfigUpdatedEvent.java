package com.pizzaconfig.commoncontracts.event;

import java.time.Instant;

/**
 * Published by admin-service whenever a rule threshold changes (CLAUDE.md §8), so every
 * rule-service replica invalidates its in-memory cache without a restart. {@code value} is
 * a JSON-encoded scalar or array whose shape depends on {@code ruleId} (e.g. "2" for
 * cheese-cap, "[\"S\",\"M\"]" for gluten-free-sizes).
 */
public record RuleConfigUpdatedEvent(
        String ruleId,
        String value,
        Instant occurredAt
) {
}

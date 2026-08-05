package com.pizzaconfig.ruleservice.thresholds;

import java.time.Instant;

public record RuleThresholdResponse(
        String ruleId,
        String value,
        Instant updatedAt
) {
    public static RuleThresholdResponse from(RuleThreshold threshold) {
        return new RuleThresholdResponse(threshold.getRuleId(), threshold.getValue(), threshold.getUpdatedAt());
    }
}

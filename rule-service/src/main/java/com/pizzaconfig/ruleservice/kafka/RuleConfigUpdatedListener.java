package com.pizzaconfig.ruleservice.kafka;

import com.pizzaconfig.commoncontracts.event.RuleConfigUpdatedEvent;
import com.pizzaconfig.ruleservice.thresholds.RuleThresholdApplier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Per CLAUDE.md §8: a rule threshold change must never require a restart and must
// propagate to every rule-service replica — each pod applies the update to its own
// in-memory RuleProperties cache as soon as it consumes this event. The shared
// rules_db itself was already updated by whichever replica handled the original
// PUT /v1/rules/thresholds/{ruleId} request (see RuleThresholdService), so this
// listener only needs to refresh this pod's in-memory copy, not write to the DB again.
@Component
public class RuleConfigUpdatedListener {

    private final RuleThresholdApplier applier;

    public RuleConfigUpdatedListener(RuleThresholdApplier applier) {
        this.applier = applier;
    }

    @KafkaListener(topics = "rule-config-updated", groupId = "rule-service")
    public void handle(RuleConfigUpdatedEvent event) {
        applier.apply(event.ruleId(), event.value());
    }
}

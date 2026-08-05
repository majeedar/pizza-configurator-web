package com.pizzaconfig.ruleservice.kafka;

import com.pizzaconfig.commoncontracts.event.RuleConfigUpdatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// Published by whichever rule-service replica actually handles a PUT
// /v1/rules/thresholds/{ruleId} request, so every *other* replica's in-memory cache
// (which a single REST call can't reach) also picks up the change immediately.
@Component
public class RuleConfigEventPublisher {

    private static final String TOPIC = "rule-config-updated";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RuleConfigEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(RuleConfigUpdatedEvent event) {
        kafkaTemplate.send(TOPIC, event.ruleId(), event);
    }
}

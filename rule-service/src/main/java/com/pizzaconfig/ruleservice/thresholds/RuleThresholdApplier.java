package com.pizzaconfig.ruleservice.thresholds;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzaconfig.ruleservice.rules.RuleProperties;
import org.springframework.stereotype.Component;

import java.util.List;

// Shared by both the Kafka-consumer path (sibling replicas catching up) and the
// REST-triggered path (the replica that actually handled the PUT) so both apply
// the same value the same way to the in-memory RuleProperties cache.
@Component
public class RuleThresholdApplier {

    private final RuleProperties ruleProperties;
    private final ObjectMapper objectMapper;

    public RuleThresholdApplier(RuleProperties ruleProperties, ObjectMapper objectMapper) {
        this.ruleProperties = ruleProperties;
        this.objectMapper = objectMapper;
    }

    public void apply(String ruleId, String value) {
        try {
            switch (ruleId) {
                case "cheese-cap" -> ruleProperties.setCheeseCap(objectMapper.readValue(value, Integer.class));
                case "gluten-free-sizes" -> ruleProperties.setGlutenFreeSizes(
                        objectMapper.readValue(value, objectMapper.getTypeFactory()
                                .constructCollectionType(List.class, String.class)));
                case "oven-thermal-limit" -> ruleProperties.setOvenThermalLimit(objectMapper.readValue(value, Integer.class));
                default -> throw new IllegalArgumentException("Unknown ruleId: " + ruleId);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to apply rule threshold for ruleId=" + ruleId, e);
        }
    }
}

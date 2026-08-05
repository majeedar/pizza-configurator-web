package com.pizzaconfig.ruleservice.thresholds;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// On startup, load current values from the shared rules_db into the in-memory
// RuleProperties cache — application.yml's `rule:` block is only the seed used by
// Flyway's initial migration, not what a running/restarted pod should trust afterward.
@Component
public class RuleThresholdInitializer implements ApplicationRunner {

    private final RuleThresholdRepository repository;
    private final RuleThresholdApplier applier;

    public RuleThresholdInitializer(RuleThresholdRepository repository, RuleThresholdApplier applier) {
        this.repository = repository;
        this.applier = applier;
    }

    @Override
    public void run(ApplicationArguments args) {
        repository.findAll().forEach(threshold -> applier.apply(threshold.getRuleId(), threshold.getValue()));
    }
}

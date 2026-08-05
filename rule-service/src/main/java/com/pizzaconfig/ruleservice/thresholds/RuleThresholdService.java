package com.pizzaconfig.ruleservice.thresholds;

import com.pizzaconfig.commoncontracts.event.RuleConfigUpdatedEvent;
import com.pizzaconfig.ruleservice.kafka.RuleConfigEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class RuleThresholdService {

    private final RuleThresholdRepository repository;
    private final RuleThresholdApplier applier;
    private final RuleConfigEventPublisher eventPublisher;

    public RuleThresholdService(RuleThresholdRepository repository, RuleThresholdApplier applier,
                                 RuleConfigEventPublisher eventPublisher) {
        this.repository = repository;
        this.applier = applier;
        this.eventPublisher = eventPublisher;
    }

    public List<RuleThreshold> findAll() {
        return repository.findAll();
    }

    @Transactional
    public Optional<RuleThreshold> updateThreshold(String ruleId, String value) {
        Optional<RuleThreshold> updated = repository.findById(ruleId).map(threshold -> {
            threshold.setValue(value);
            threshold.setUpdatedAt(Instant.now());
            return threshold;
        });

        updated.ifPresent(threshold -> {
            applier.apply(ruleId, value);
            eventPublisher.publish(new RuleConfigUpdatedEvent(ruleId, value, Instant.now()));
        });

        return updated;
    }
}

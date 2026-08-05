package com.pizzaconfig.adminservice.service;

import com.pizzaconfig.adminservice.client.RuleServiceClient;
import com.pizzaconfig.adminservice.client.RuleServiceClient.RuleThresholdDto;
import com.pizzaconfig.adminservice.domain.AuditLogEntry;
import com.pizzaconfig.adminservice.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AdminRuleService {

    private static final String ACTION_TYPE = "RULE_UPDATE";

    private final RuleServiceClient ruleServiceClient;
    private final AuditLogRepository auditLogRepository;

    public AdminRuleService(RuleServiceClient ruleServiceClient, AuditLogRepository auditLogRepository) {
        this.ruleServiceClient = ruleServiceClient;
        this.auditLogRepository = auditLogRepository;
    }

    public List<RuleThresholdDto> findAll() {
        return ruleServiceClient.findAll();
    }

    @Transactional
    public Optional<RuleThresholdDto> updateRule(String ruleId, String value) {
        String previousValue = ruleServiceClient.findAll().stream()
                .filter(threshold -> threshold.ruleId().equals(ruleId))
                .findFirst()
                .map(RuleThresholdDto::value)
                .orElse(null);

        Optional<RuleThresholdDto> updated = ruleServiceClient.updateThreshold(ruleId, value);

        updated.ifPresent(threshold -> auditLogRepository.save(new AuditLogEntry(
                ACTION_TYPE, ruleId, previousValue, value, Instant.now())));

        return updated;
    }
}

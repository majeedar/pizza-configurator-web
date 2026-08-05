package com.pizzaconfig.adminservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

// REST/JSON for this internal call as a deliberate scope decision for clarity (see
// CLAUDE.md §7) — gRPC is the documented upgrade path for lower internal latency.
// admin-service no longer stores rule thresholds itself; rule-service (rules_db) is
// the sole owner. rule-service's PUT endpoint is responsible for publishing
// RuleConfigUpdated so every rule-service replica's cache stays in sync (§8) — a
// single REST call from here can only ever reach one replica.
@Component
public class RuleServiceClient {

    private final RestClient restClient;

    public RuleServiceClient(RestClient.Builder builder,
                              @Value("${clients.rule-service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public List<RuleThresholdDto> findAll() {
        RuleThresholdDto[] items = restClient.get()
                .uri("/v1/rules/thresholds")
                .retrieve()
                .body(RuleThresholdDto[].class);
        return items == null ? List.of() : Arrays.asList(items);
    }

    public Optional<RuleThresholdDto> updateThreshold(String ruleId, String value) {
        try {
            RuleThresholdDto result = restClient.put()
                    .uri("/v1/rules/thresholds/{ruleId}", ruleId)
                    .body(new UpdateThresholdRequest(value))
                    .retrieve()
                    .body(RuleThresholdDto.class);
            return Optional.ofNullable(result);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public record RuleThresholdDto(String ruleId, String value, Instant updatedAt) {
    }

    public record UpdateThresholdRequest(String value) {
    }
}

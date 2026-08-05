package com.pizzaconfig.orderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

// REST/JSON for this internal call as a deliberate scope decision for clarity (see
// CLAUDE.md §7) — gRPC is the documented upgrade path for lower internal latency.
// Used only by PendingReviewService to validate a staff-proposed resolution before it's
// ever shown to the customer — the customer's confirm step never re-validates.
@Component
public class RuleServiceClient {

    private final RestClient restClient;

    public RuleServiceClient(RestClient.Builder builder, @Value("${clients.rule-service.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public ValidationResultDto validate(ChangeRequestDto request) {
        return restClient.post()
                .uri("/v1/rules/validate")
                .body(request)
                .retrieve()
                .body(ValidationResultDto.class);
    }

    public record AdditionDto(String ingredientId, String type, int quantity) {
    }

    public record ChangeRequestDto(
            String basePizzaId,
            String size,
            String dough,
            List<AdditionDto> additions,
            List<String> removals,
            String source,
            boolean ambiguous
    ) {
    }

    public record FailedRuleDto(String ruleId, String messageDe) {
    }

    public record ValidationResultDto(String outcome, List<FailedRuleDto> failures) {
    }
}

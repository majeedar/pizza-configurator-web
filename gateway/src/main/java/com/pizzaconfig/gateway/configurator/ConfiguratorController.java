package com.pizzaconfig.gateway.configurator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

// Orchestrates the free-text path per CLAUDE.md §5: "Gateway -> ai-parser-service
// first, which returns a structured proposal; that proposal is what gets validated."
// The frontend calls only this one endpoint — it never talks to ai-parser-service or
// rule-service directly, so free-text parsing is a backend concern, not something the
// UI has to explicitly trigger as a separate step.
//
// When ai-parser-service can't confidently map the comment to a structured change
// (ambiguous=true), we don't force its uncertain guess through rule validation —
// instead the raw comment is passed through as a kitchen note, and the button-selected
// configuration is validated unchanged. A confident parse fully replaces the
// button-selected config and is validated as free text.
@RestController
@RequestMapping("/v1/customer/configurator")
public class ConfiguratorController {

    private final RestClient aiParserClient;
    private final RestClient ruleServiceClient;

    public ConfiguratorController(RestClient.Builder builder,
                                   @Value("${clients.ai-parser-service.base-url}") String aiParserBaseUrl,
                                   @Value("${clients.rule-service.base-url}") String ruleServiceBaseUrl) {
        this.aiParserClient = builder.baseUrl(aiParserBaseUrl).build();
        this.ruleServiceClient = builder.baseUrl(ruleServiceBaseUrl).build();
    }

    @PostMapping("/validate")
    public ConfigureResponse validate(@RequestBody ConfigureRequest request) {
        String basePizzaId = request.basePizzaId();
        String size = request.size();
        String dough = request.dough();
        List<AdditionDto> additions = request.additions() == null ? List.of() : request.additions();
        List<String> removals = request.removals() == null ? List.of() : request.removals();
        String source = "BUTTON";
        boolean ambiguous = false;
        String kitchenNote = null;

        if (request.comment() != null && !request.comment().isBlank()) {
            ParsedProposal proposal = aiParserClient.post()
                    .uri("/v1/ai-parser/parse")
                    .body(new ParseRequest(basePizzaId, size, dough, request.comment()))
                    .retrieve()
                    .body(ParsedProposal.class);

            if (proposal != null) {
                ambiguous = proposal.ambiguous();
                if (ambiguous) {
                    kitchenNote = request.comment();
                } else {
                    basePizzaId = proposal.basePizzaId();
                    size = proposal.size();
                    dough = proposal.dough();
                    additions = proposal.additions();
                    removals = proposal.removals();
                    source = "FREE_TEXT";
                }
            }
        }

        Map<String, Object> validation = ruleServiceClient.post()
                .uri("/v1/rules/validate")
                .body(new ChangeRequest(basePizzaId, size, dough, additions, removals, source, false))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });

        return new ConfigureResponse(basePizzaId, size, dough, additions, removals, ambiguous, kitchenNote, validation);
    }
}

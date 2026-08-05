package com.pizzaconfig.aiparserservice.service;

import com.pizzaconfig.aiparserservice.client.OpenAiClient;
import com.pizzaconfig.aiparserservice.dto.ParseRequest;
import com.pizzaconfig.aiparserservice.dto.ParsedProposal;
import org.springframework.stereotype.Service;

@Service
public class AiParserService {

    private static final String SYSTEM_PROMPT = """
            You translate a customer's free-text pizza order comment into a structured
            modification proposal. You have zero knowledge of business rules (cheese limits,
            size constraints, allowed combinations, etc.) — only extract what the customer
            literally asked for; a separate rule engine will validate it.

            Respond with a single JSON object matching exactly this shape, nothing else:
            {
              "basePizzaId": string,
              "size": string,
              "dough": string,
              "additions": [{"ingredientId": string, "type": "CHEESE" | "TOPPING", "quantity": number}],
              "removals": [string],
              "ambiguous": boolean
            }

            Set "ambiguous" to true only if the comment cannot be confidently mapped to a
            single clear intent (e.g. it's contradictory or missing essential information).
            """;

    private final OpenAiClient openAiClient;

    public AiParserService(OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    public ParsedProposal parse(ParseRequest request) {
        String userPrompt = """
                Current selection: basePizzaId=%s, size=%s, dough=%s
                Customer comment: "%s"
                """.formatted(request.basePizzaId(), request.size(), request.dough(), request.comment());

        return openAiClient.parse(SYSTEM_PROMPT, userPrompt);
    }
}

package com.pizzaconfig.aiparserservice.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzaconfig.aiparserservice.dto.ParsedProposal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OpenAiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiClient(RestClient.Builder builder,
                         ObjectMapper objectMapper,
                         @Value("${openai.base-url}") String baseUrl,
                         @Value("${openai.api-key}") String apiKey,
                         @Value("${openai.model}") String model) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.objectMapper = objectMapper;
        this.model = model;
    }

    public ParsedProposal parse(String systemPrompt, String userPrompt) {
        OpenAiRequest request = new OpenAiRequest(
                model,
                List.of(new OpenAiMessage("system", systemPrompt), new OpenAiMessage("user", userPrompt)),
                new OpenAiResponseFormat("json_object")
        );

        OpenAiChatResponse response = restClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(OpenAiChatResponse.class);

        if (response == null || response.choices().isEmpty()) {
            throw new IllegalStateException("OpenAI returned no choices");
        }

        String content = response.choices().get(0).message().content();
        try {
            return objectMapper.readValue(content, ParsedProposal.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI parser returned a proposal that didn't match the expected JSON shape", e);
        }
    }
}

package com.pizzaconfig.aiparserservice.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OpenAiRequest(
        String model,
        List<OpenAiMessage> messages,
        @JsonProperty("response_format") OpenAiResponseFormat responseFormat
) {
}

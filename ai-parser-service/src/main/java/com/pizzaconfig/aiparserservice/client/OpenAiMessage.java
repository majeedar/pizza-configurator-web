package com.pizzaconfig.aiparserservice.client;

public record OpenAiMessage(
        String role,
        String content
) {
}

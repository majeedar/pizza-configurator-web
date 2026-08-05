package com.pizzaconfig.aiparserservice.client;

import java.util.List;

public record OpenAiChatResponse(
        List<Choice> choices
) {
    public record Choice(OpenAiMessage message) {
    }
}

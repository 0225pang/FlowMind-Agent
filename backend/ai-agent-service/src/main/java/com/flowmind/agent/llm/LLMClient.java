package com.flowmind.agent.llm;

import java.util.function.Consumer;

public interface LLMClient {
    String complete(String systemPrompt, String userPrompt);

    default void stream(String systemPrompt, String userPrompt, Consumer<String> onDelta) {
        onDelta.accept(complete(systemPrompt, userPrompt));
    }
}

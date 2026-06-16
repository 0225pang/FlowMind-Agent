package com.flowmind.agent.core;

import com.flowmind.agent.dto.AgentRequest;
import com.flowmind.agent.dto.AgentResponse;

import java.util.function.Consumer;

public interface Agent {
    String getName();

    String getDescription();

    AgentResponse execute(AgentRequest request);

    void stream(AgentRequest request, Consumer<String> onDelta);
}

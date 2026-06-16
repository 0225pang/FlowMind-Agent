package com.flowmind.agent.extension;

import com.flowmind.agent.dto.AgentRequest;

public interface AgentExtension {
    String name();

    String type();

    String description();

    default boolean supports(String agentType) {
        return true;
    }

    default String runtimeContext(AgentRequest request) {
        return "";
    }
}

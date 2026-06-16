package com.flowmind.agent.extension;

public interface McpToolProvider extends AgentExtension {
    @Override
    default String type() {
        return "mcp";
    }
}

package com.flowmind.agent.extension;

import org.springframework.stereotype.Component;

/**
 * Capability metadata only. The actual implementation lives in knowledge-service.
 * This lets the main agent discover the ability without coupling AgentRouter to it.
 */
@Component
public class SemanticVectorSearchExtension implements McpToolProvider {
    @Override
    public String name() {
        return "semantic-vector-search";
    }

    @Override
    public String description() {
        return "Use GET /api/knowledge/vector/search?q={query}&topK=5 for decoupled semantic retrieval from the knowledge base.";
    }

    @Override
    public boolean supports(String agentType) {
        return "knowledge".equals(agentType)
                || "content".equals(agentType)
                || "auto".equals(agentType);
    }
}

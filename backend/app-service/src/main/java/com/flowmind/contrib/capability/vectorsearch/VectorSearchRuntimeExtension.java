package com.flowmind.contrib.capability.vectorsearch;

import com.flowmind.agent.dto.AgentRequest;
import com.flowmind.agent.extension.McpToolProvider;
import com.flowmind.knowledge.vector.VectorSearchToolService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runtime bridge between the agent prompt and the knowledge-service vector search tool.
 *
 * This class lives in app-service because app-service depends on both ai-agent-service
 * and knowledge-service. It lets the team add a real callable capability without
 * changing AgentRouter or any existing Agent implementation.
 */
@Component
public class VectorSearchRuntimeExtension implements McpToolProvider {
    private final VectorSearchToolService vectorSearchToolService;

    @Value("${flowmind.vector-demo.agent-enabled:true}")
    private boolean agentEnabled;

    @Value("${flowmind.vector-demo.agent-top-k:5}")
    private int agentTopK;

    @Value("${flowmind.vector-demo.always-search:true}")
    private boolean alwaysSearch;

    public VectorSearchRuntimeExtension(VectorSearchToolService vectorSearchToolService) {
        this.vectorSearchToolService = vectorSearchToolService;
    }

    @Override
    public String name() {
        return "semantic-vector-search-runtime";
    }

    @Override
    public String description() {
        return "Real runtime vector retrieval. By default every user question is searched against the knowledge vector database first. Relevant results are injected into the agent context.";
    }

    @Override
    public boolean supports(String agentType) {
        return true;
    }

    @Override
    public String runtimeContext(AgentRequest request) {
        if (!agentEnabled || request == null || request.getMessage() == null) {
            return "";
        }

        String query = request.getMessage().trim();
        if (query.isBlank() || !shouldRetrieve(query)) {
            return "";
        }

        List<VectorSearchToolService.VectorSearchResult> results =
                vectorSearchToolService.search(query, agentTopK);
        if (results.isEmpty()) {
            return "Vector retrieval was called first, but no relevant knowledge-base result was found for query: " + query
                    + "\nInstruction: do not invent knowledge-base facts. If the answer needs project-local facts, say that no matching knowledge-base record was found.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Vector retrieval was called first and returned candidate facts.\n");
        builder.append("Query: ").append(query).append("\n");
        builder.append("Instruction: use these facts when relevant. If they are not relevant enough, explicitly say the knowledge base did not provide enough evidence. Do not fabricate missing facts.\n\n");

        for (int i = 0; i < results.size(); i++) {
            VectorSearchToolService.VectorSearchResult item = results.get(i);
            builder.append(i + 1).append(". ")
                    .append(nullToEmpty(item.title()))
                    .append(" [source=").append(nullToEmpty(item.source())).append("]\n");
            if (item.distance() != null) {
                builder.append("   distance: ").append(item.distance()).append("\n");
            }
            if (item.feishuUrl() != null && !item.feishuUrl().isBlank()) {
                builder.append("   url: ").append(item.feishuUrl()).append("\n");
            }
            builder.append("   snippet: ")
                    .append(trim(nullToEmpty(item.chunkText()), 700))
                    .append("\n");
        }
        return builder.toString();
    }

    private boolean shouldRetrieve(String text) {
        if (alwaysSearch) {
            return true;
        }
        String lower = text.toLowerCase();
        return containsAny(lower,
                "知识库", "向量", "检索", "搜索", "查找", "资料", "文档", "rag",
                "根据知识", "根据资料", "根据文档", "保研知识库",
                "knowledge", "vector", "retrieve", "retrieval", "search");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String trim(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

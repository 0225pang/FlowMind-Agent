package com.flowmind.agent.core;

import com.flowmind.agent.dto.AgentRequest;
import com.flowmind.agent.dto.AgentResponse;
import com.flowmind.agent.extension.AgentExtension;
import com.flowmind.agent.llm.LLMClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KnowledgeAgent extends BaseAgent {
    public KnowledgeAgent(LLMClient llm, List<AgentExtension> extensions) {
        super(llm, extensions);
    }

    public String getName() {
        return "KnowledgeAgent";
    }

    public String getDescription() {
        return "资料整理、文档摘要、标签提取、内容资产结构化和知识问答助手。";
    }

    public AgentResponse execute(AgentRequest request) {
        return response("knowledge", request.getMessage(), List.of(
                Map.of("title", "资料整理", "content", "按主题、来源、使用场景和可信度拆分知识条目"),
                Map.of("title", "标签提取", "content", "自动提取主题、目的、风格、转化标签"),
                Map.of("title", "向量检索预留", "content", "后续可接 Milvus、pgvector、Qdrant 或 Elasticsearch")
        ));
    }
}

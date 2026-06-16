package com.flowmind.agent.extension;

import org.springframework.stereotype.Component;

@Component
class FeishuMcpExtension implements McpToolProvider {
    public String name() {
        return "feishu-mock-mcp";
    }

    public String description() {
        return "预留飞书文档、多维表格、任务和群机器人工具调用入口。";
    }
}

@Component
class ContentSkillExtension implements SkillProvider {
    public String name() {
        return "content-sop-skill";
    }

    public String description() {
        return "预留小红书爆款仿写、朋友圈人设表达和内容资产沉淀 SOP。";
    }

    public boolean supports(String agentType) {
        return "content".equals(agentType) || "knowledge".equals(agentType);
    }
}

@Component
class VectorSearchMcpExtension implements McpToolProvider {
    public String name() {
        return "vector-search-mcp";
    }

    public String description() {
        return "预留向量数据库知识检索能力，可替换为 Milvus、Qdrant、pgvector 或 Elasticsearch。";
    }
}

package com.flowmind.agent.core;

import com.flowmind.agent.dto.AgentRequest;
import com.flowmind.agent.dto.AgentResponse;
import com.flowmind.agent.extension.AgentExtension;
import com.flowmind.agent.llm.LLMClient;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class BaseAgent implements Agent {
    protected final LLMClient llm;
    private final List<AgentExtension> extensions;

    protected BaseAgent(LLMClient llm, List<AgentExtension> extensions) {
        this.llm = llm;
        this.extensions = extensions == null ? List.of() : extensions;
    }

    protected AgentResponse response(String type, String prompt, List<Map<String, Object>> cards) {
        AgentRequest request = new AgentRequest();
        request.setAgentType(type);
        request.setMessage(prompt);
        return new AgentResponse(type, llm.complete(systemPrompt(type, request), prompt), cards);
    }

    @Override
    public void stream(AgentRequest request, Consumer<String> onDelta) {
        String type = getName().replace("Agent", "").toLowerCase();
        llm.stream(systemPrompt(type, request), request.getMessage(), onDelta);
    }

    protected String systemPrompt(String agentType, AgentRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是 FlowMind Agent 平台中的 ").append(getName()).append("。\n");
        builder.append(getDescription()).append("\n\n");
        builder.append("请用中文回答，结构清晰，优先给出可执行步骤、产出格式和下一步动作。\n");
        builder.append("如果工具上下文提供了当前时间、联网检索结果或 URL 内容，请优先使用这些事实，不要说自己无法获取。\n");
        builder.append("不要编造真实隐私，不要声称已经写入外部系统，除非上下文明确提供了调用结果。\n");
        builder.append("当前可扩展能力：LLM API、MCP 服务、Skill、飞书文档、飞书多维表格、向量数据库、本地 MySQL。\n");

        // Inject conversation history from context
        if (request.getContext() != null && request.getContext().containsKey("conversationHistory")) {
            builder.append("\n").append(request.getContext().get("conversationHistory"));
            builder.append("\n请根据以上对话历史记住用户之前说过的信息（如姓名、偏好、上下文等），在回答时自然引用。\n");
        }

        List<AgentExtension> matched = extensions.stream()
                .filter(extension -> extension.supports(agentType))
                .toList();
        if (!matched.isEmpty()) {
            builder.append("\n已注册扩展能力：\n");
            for (AgentExtension extension : matched) {
                builder.append("- [").append(extension.type()).append("] ")
                        .append(extension.name()).append(": ")
                        .append(extension.description()).append("\n");
            }
        }

        StringBuilder runtime = new StringBuilder();
        for (AgentExtension extension : matched) {
            String context = extension.runtimeContext(request);
            if (context != null && !context.isBlank()) {
                runtime.append("\n[").append(extension.type()).append(":")
                        .append(extension.name()).append("]\n")
                        .append(context).append("\n");
            }
        }
        if (!runtime.isEmpty()) {
            builder.append("\n工具上下文：\n").append(runtime);
        }
        return builder.toString();
    }
}

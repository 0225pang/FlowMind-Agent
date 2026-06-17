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

    @Override
    public void stream(AgentRequest request, Consumer<String> onDelta, Consumer<String> onReasoningDelta) {
        String type = getName().replace("Agent", "").toLowerCase();
        llm.stream(systemPrompt(type, request), request.getMessage(), onDelta, onReasoningDelta);
    }

    protected String systemPrompt(String agentType, AgentRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是 FlowMind Agent 平台中的 ").append(getName()).append("。\n");
        builder.append(getDescription()).append("\n\n");
        builder.append("请用中文回答，结构清晰，优先给出可执行步骤、产出格式和下一步动作。\n");
        builder.append("回答优先级：1. 工具上下文中的向量检索结果；2. 其他工具结果，例如时间、联网搜索、飞书工具；3. 你的一般知识。\n");
        builder.append("如果向量检索没有命中，或者命中的内容与问题不相关，请明确说明“知识库没有找到足够相关的记录”，再结合其他可用能力回答。\n");
        builder.append("不要编造知识库、飞书、数据库或联网搜索中不存在的事实。不要声称已经写入外部系统，除非工具上下文明示调用成功。\n");
        builder.append("联网搜索结果只作为线索，不自动视为权威事实；涉及最新、政策、价格、院校通知等信息时，优先提示用户核验官方来源。\n");
        builder.append("当前可扩展能力：LLM API、MCP 服务、Skill、飞书文档、飞书多维表格、向量数据库、本地 MySQL、联网搜索。\n");

        if (request.getContext() != null && request.getContext().containsKey("conversationHistory")) {
            builder.append("\n").append(request.getContext().get("conversationHistory"));
            builder.append("\n请根据以上对话历史记住用户之前说过的信息，并在回答时自然引用。\n");
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

        String runtimeContext = cachedRuntimeContext(request);
        if (runtimeContext == null) {
            StringBuilder runtime = new StringBuilder();
            for (AgentExtension extension : matched) {
                String context = extension.runtimeContext(request);
                if (context != null && !context.isBlank()) {
                    runtime.append("\n[").append(extension.type()).append(":")
                            .append(extension.name()).append("]\n")
                            .append(context).append("\n");
                }
            }
            runtimeContext = runtime.toString();
        }
        if (!runtimeContext.isBlank()) {
            builder.append("\n工具上下文：\n").append(runtimeContext);
        }
        return builder.toString();
    }

    private String cachedRuntimeContext(AgentRequest request) {
        if (request.getContext() == null) return null;
        Object value = request.getContext().get("runtimeToolContext");
        return value == null ? null : String.valueOf(value);
    }
}

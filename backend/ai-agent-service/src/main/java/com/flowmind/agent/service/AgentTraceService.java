package com.flowmind.agent.service;

import com.flowmind.agent.dto.AgentRequest;
import com.flowmind.agent.extension.AgentExtension;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentTraceService {
    private final List<AgentExtension> extensions;

    public AgentTraceService(List<AgentExtension> extensions) {
        this.extensions = extensions == null ? List.of() : extensions;
    }

    public TraceBundle collect(AgentRequest request, String agentType) {
        List<Map<String, Object>> items = new ArrayList<>();
        StringBuilder runtimeContext = new StringBuilder();

        List<AgentExtension> matched = extensions.stream()
                .filter(extension -> extension.supports(agentType))
                .toList();

        for (AgentExtension extension : matched) {
            long started = System.currentTimeMillis();
            String context = "";
            String status = "skipped";
            try {
                context = extension.runtimeContext(request);
                status = context == null || context.isBlank() ? "skipped" : "used";
            } catch (Exception e) {
                status = "failed";
                context = "Tool failed: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }

            if (context != null && !context.isBlank()) {
                runtimeContext.append("\n[").append(extension.type()).append(":")
                        .append(extension.name()).append("]\n")
                        .append(context).append("\n");
            }

            Map<String, Object> item = new HashMap<>();
            item.put("name", extension.name());
            item.put("type", extension.type());
            item.put("status", status);
            item.put("durationMs", System.currentTimeMillis() - started);
            item.put("summary", summarize(context));
            item.put("detail", context == null ? "" : context);
            items.add(item);
        }

        Map<String, Object> c = request.getContext();
        if (c == null) c = new HashMap<>();
        c.put("runtimeToolContext", runtimeContext.toString());
        c.put("traceItems", items);
        request.setContext(c);

        return new TraceBundle(items, reasoningSummary(items, agentType), runtimeContext.toString());
    }

    private String summarize(String context) {
        if (context == null || context.isBlank()) {
            return "未触发。";
        }
        String firstLine = context.lines()
                .filter(line -> line != null && !line.isBlank())
                .findFirst()
                .orElse(context);
        return trim(firstLine, 160);
    }

    private String reasoningSummary(List<Map<String, Object>> items, String agentType) {
        long used = items.stream().filter(item -> "used".equals(item.get("status"))).count();
        StringBuilder builder = new StringBuilder();
        builder.append("本轮先选择 Agent：").append(agentType).append("。\n");
        builder.append("随后先检查向量知识库，再检查飞书、联网搜索、时间等扩展能力。\n");
        if (used == 0) {
            builder.append("没有工具返回可用上下文，回答会主要依赖模型通用知识，并提示不确定处。");
        } else {
            builder.append("共有 ").append(used).append(" 个工具返回上下文，模型回答会优先基于这些上下文生成。");
        }
        builder.append("\n生成时间：").append(LocalDateTime.now()).append("。");
        return builder.toString();
    }

    private String trim(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    public record TraceBundle(List<Map<String, Object>> items, String reasoning, String runtimeContext) {}
}

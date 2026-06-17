package com.flowmind.agent.service;

import com.flowmind.agent.dto.AgentRequest;
import com.flowmind.agent.extension.AgentExtension;
import org.springframework.stereotype.Service;

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
        List<Map<String, Object>> visibleItems = new ArrayList<>();
        StringBuilder runtimeContext = new StringBuilder();

        List<AgentExtension> matched = extensions.stream()
                .filter(extension -> extension.supports(agentType))
                .toList();

        for (AgentExtension extension : matched) {
            long started = System.currentTimeMillis();
            String context = "";
            String status;
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

            if (!"skipped".equals(status)) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", extension.name());
                item.put("type", extension.type());
                item.put("status", status);
                item.put("durationMs", System.currentTimeMillis() - started);
                item.put("summary", summarize(context));
                item.put("detail", context == null ? "" : context);
                visibleItems.add(item);
            }
        }

        Map<String, Object> c = request.getContext();
        if (c == null) c = new HashMap<>();
        c.put("runtimeToolContext", runtimeContext.toString());
        c.put("traceItems", visibleItems);
        request.setContext(c);

        return new TraceBundle(visibleItems, thinkingStatus(visibleItems, agentType), runtimeContext.toString());
    }

    private String summarize(String context) {
        if (context == null || context.isBlank()) {
            return "未返回上下文。";
        }
        String firstLine = context.lines()
                .filter(line -> line != null && !line.isBlank())
                .findFirst()
                .orElse(context);
        return trim(firstLine, 160);
    }

    private String thinkingStatus(List<Map<String, Object>> items, String agentType) {
        if (items.isEmpty()) {
            return "已选择 " + agentType + "，未获得外部工具上下文，正在生成回答。";
        }
        long used = items.stream().filter(item -> "used".equals(item.get("status"))).count();
        long failed = items.stream().filter(item -> "failed".equals(item.get("status"))).count();
        return "已选择 " + agentType + "，调用 " + used + " 个工具"
                + (failed > 0 ? "，" + failed + " 个工具失败" : "")
                + "，正在基于可用结果生成回答。";
    }

    private String trim(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    public record TraceBundle(List<Map<String, Object>> items, String thinking, String runtimeContext) {}
}

package com.flowmind.agent.extension.xiaohongshu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmind.agent.llm.LLMClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class XiaohongshuKeywordPlanner {
    private final LLMClient llm;
    private final ObjectMapper objectMapper;

    public XiaohongshuKeywordPlanner(LLMClient llm, ObjectMapper objectMapper) {
        this.llm = llm;
        this.objectMapper = objectMapper;
    }

    public KeywordPlan plan(String topic, int maxKeywords, boolean useLlm) {
        String safeTopic = topic == null || topic.isBlank() ? "保研" : topic.trim();
        int safeMax = Math.max(1, Math.min(maxKeywords, 12));
        if (!useLlm) {
            return new KeywordPlan(false, "local keyword expansion", fallbackKeywords(safeTopic, safeMax));
        }
        try {
            List<String> generated = parseKeywords(llm.complete(systemPrompt(), userPrompt(safeTopic, safeMax)));
            if (!generated.isEmpty()) {
                return new KeywordPlan(true, "llm keyword expansion", normalize(generated, safeTopic, safeMax));
            }
        } catch (Exception ignored) {
            // LLM keyword planning is an enhancement. Search must still work without it.
        }
        return new KeywordPlan(false, "llm keyword expansion failed; local fallback was used", fallbackKeywords(safeTopic, safeMax));
    }

    private String systemPrompt() {
        return """
                You are a keyword planner for Xiaohongshu content-operation research.
                Return only a strict JSON array of Chinese search keywords.
                Keywords should be close to the user's topic, useful for finding hot posts, and not contain explanations.
                """;
    }

    private String userPrompt(String topic, int maxKeywords) {
        return "主题：" + topic + "\n请生成 " + maxKeywords + " 个适合小红书搜索的相近关键词，只返回 JSON 数组。";
    }

    private List<String> parseKeywords(String raw) throws Exception {
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        JsonNode node = objectMapper.readTree(text);
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> {
                String keyword = item.asText("").trim();
                if (!keyword.isBlank()) {
                    values.add(keyword);
                }
            });
        }
        return values;
    }

    private List<String> fallbackKeywords(String topic, int maxKeywords) {
        List<String> values = new ArrayList<>();
        values.add(topic);
        if (topic.contains("保研") || topic.contains("推免")) {
            values.addAll(List.of("保研经验", "保研面试", "保研简历", "保研文书", "推免", "夏令营保研", "预推免", "导师套磁", "保研材料"));
        } else {
            values.add(topic + "经验");
            values.add(topic + "攻略");
            values.add(topic + "避坑");
            values.add(topic + "干货");
            values.add(topic + "案例");
            values.add(topic + "模板");
            values.add(topic + "复盘");
            values.add(topic + "清单");
        }
        return normalize(values, topic, maxKeywords);
    }

    private List<String> normalize(List<String> keywords, String topic, int maxKeywords) {
        Set<String> values = new LinkedHashSet<>();
        values.add(topic);
        for (String keyword : keywords) {
            String value = keyword == null ? "" : keyword.trim();
            if (!value.isBlank() && value.length() <= 30) {
                values.add(value);
            }
        }
        return values.stream().limit(maxKeywords).toList();
    }

    public record KeywordPlan(boolean llmUsed, String message, List<String> keywords) {
    }
}

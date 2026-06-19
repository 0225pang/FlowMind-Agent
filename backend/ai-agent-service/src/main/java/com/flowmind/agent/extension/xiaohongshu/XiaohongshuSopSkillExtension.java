package com.flowmind.agent.extension.xiaohongshu;

import com.flowmind.agent.dto.AgentRequest;
import com.flowmind.agent.extension.SkillProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class XiaohongshuSopSkillExtension implements SkillProvider {
    private static final Pattern TOPIC_PATTERN = Pattern.compile("(?:主题|选题|关键词|小红书|搜索|仿写|生成)[:：]?\\s*([^，。；;\\n]{2,40})");

    private final XiaohongshuMcpClient client;
    private final XiaohongshuMcpToolRegistry registry;

    @Value("${flowmind.tools.xiaohongshu-mcp.agent-enabled:true}")
    private boolean agentEnabled;

    @Value("${flowmind.tools.xiaohongshu-mcp.default-limit:8}")
    private int defaultLimit;

    public XiaohongshuSopSkillExtension(XiaohongshuMcpClient client, XiaohongshuMcpToolRegistry registry) {
        this.client = client;
        this.registry = registry;
    }

    @Override
    public String name() {
        return "xiaohongshu-sop-skill";
    }

    @Override
    public String description() {
        return "Search Xiaohongshu-style hot notes for a topic, extract viral structures, and generate FlowMind content-operation SOP context. Only read/search capabilities are visible to the agent.";
    }

    @Override
    public boolean supports(String agentType) {
        return "content".equals(agentType) || "auto".equals(agentType);
    }

    @Override
    public String runtimeContext(AgentRequest request) {
        if (!agentEnabled || request == null || request.getMessage() == null) return "";
        String message = request.getMessage();
        if (!isXhsContentIntent(message)) return "";

        String topic = extractTopic(message);
        XiaohongshuMcpClient.SearchResult search = client.searchHotNotes(topic, defaultLimit);
        return buildSopContext(search);
    }

    private boolean isXhsContentIntent(String message) {
        String text = message.toLowerCase(Locale.ROOT);
        return containsAny(text,
                "小红书", "xhs", "rednote", "爆款", "高赞", "热点帖子", "热门帖子",
                "仿写", "笔记", "选题", "标题", "内容运营", "种草", "引流");
    }

    private String extractTopic(String message) {
        Matcher matcher = TOPIC_PATTERN.matcher(message);
        if (matcher.find()) {
            String candidate = cleanupTopic(matcher.group(1));
            if (!candidate.isBlank()) return candidate;
        }
        String text = message
                .replace("小红书", " ")
                .replace("爆款", " ")
                .replace("仿写", " ")
                .replace("生成", " ")
                .replace("搜索", " ")
                .replace("热点帖子", " ")
                .replace("热门帖子", " ")
                .replace("笔记", " ")
                .trim();
        return cleanupTopic(text).isBlank() ? "保研经验" : cleanupTopic(text);
    }

    private String cleanupTopic(String value) {
        String text = value == null ? "" : value.trim();
        text = text.replaceAll("^(关于|围绕|帮我|请|给我|做一个|写一篇|来一篇)", "");
        text = text.replaceAll("(的)?(小红书|爆款|笔记|文案|内容|选题|标题|仿写).*$", "");
        text = text.replaceAll("[\"'“”‘’]", "");
        text = text.trim();
        return text.length() > 40 ? text.substring(0, 40) : text;
    }

    private String buildSopContext(XiaohongshuMcpClient.SearchResult search) {
        List<XiaohongshuMcpClient.HotNote> notes = search.notes().stream()
                .sorted(Comparator.comparingLong(this::heatScore).reversed())
                .toList();

        StringBuilder builder = new StringBuilder();
        builder.append("Xiaohongshu SOP Skill was used.\n");
        builder.append("Mode: ").append(search.mode()).append("\n");
        builder.append("Message: ").append(search.message()).append("\n");
        builder.append("Topic: ").append(search.topic()).append("\n");
        builder.append("Visible MCP tools: ").append(registry.exposedTools().stream().map(XiaohongshuMcpToolRegistry.ToolSpec::name).toList()).append("\n");
        builder.append("Disabled high-risk tools are not available to the agent: ")
                .append(registry.disabledTools().stream().map(XiaohongshuMcpToolRegistry.ToolSpec::name).toList())
                .append("\n\n");

        builder.append("Hot note samples:\n");
        int index = 1;
        for (XiaohongshuMcpClient.HotNote note : notes) {
            builder.append(index++).append(". ")
                    .append("Title: ").append(empty(note.title(), "Untitled")).append("\n")
                    .append("   Author: ").append(empty(note.author(), "unknown")).append("\n")
                    .append("   Engagement: likes=").append(note.likeCount())
                    .append(", collects=").append(note.collectCount())
                    .append(", comments=").append(note.commentCount()).append("\n")
                    .append("   Tags: ").append(note.tags()).append("\n")
                    .append("   Structure clue: ").append(empty(note.summary(), "No summary")).append("\n");
            if (note.url() != null && !note.url().isBlank()) {
                builder.append("   URL: ").append(note.url()).append("\n");
            }
        }

        builder.append("\nRequired Xiaohongshu content SOP:\n");
        builder.append("Step 1. Hot-structure retrieval: infer title type, opening hook, body structure and conversion ending from the hot note samples.\n");
        builder.append("Step 2. Compress into a reusable template: title formula, opening formula, section layout and ending CTA.\n");
        builder.append("Step 3. Generate original content: imitate structure only, do not copy sentences, examples or private data.\n");
        builder.append("Step 4. Generate 10 alternative titles: anxiety, result, contrast, number, checklist and experience-summary styles.\n");
        builder.append("Step 5. Output 3 versions: dry-guide version, emotion-enhanced version and conversion-guided version.\n");
        builder.append("Step 6. Provide asset fields for Feishu/Base storage: topic, style, title, body, tags, template, image suggestion, usage status and rating.\n");
        builder.append("\nSafety rules:\n");
        builder.append("- Do not claim that content was published, liked, commented or collected.\n");
        builder.append("- Do not expose or request Xiaohongshu cookies, tokens or private account data.\n");
        builder.append("- If real MCP data is unavailable, clearly state that mock hot-note structures were used for demo generation.\n");
        return builder.toString();
    }

    private long heatScore(XiaohongshuMcpClient.HotNote note) {
        return note.likeCount() * 3 + note.collectCount() * 4 + note.commentCount() * 5;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String empty(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

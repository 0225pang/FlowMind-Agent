package com.flowmind.agent.extension.xiaohongshu;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Capability registry adapted from the xiaohongshu-mcp idea.
 *
 * FlowMind intentionally exposes only the content research path to the agent.
 * Publishing, commenting, liking and collecting are kept as disabled internal
 * capabilities so the codebase can be extended later without letting the LLM
 * call high-risk actions in the current demo.
 */
@Component
public class XiaohongshuMcpToolRegistry {
    private final List<ToolSpec> tools = List.of(
            new ToolSpec("xhs.login.status", "Check Xiaohongshu login status", RiskLevel.SAFE_READ, true),
            new ToolSpec("xhs.note.search", "Search Xiaohongshu notes by keyword/topic", RiskLevel.SAFE_READ, true),
            new ToolSpec("xhs.note.detail", "Read public note detail by note id/link", RiskLevel.SAFE_READ, true),
            new ToolSpec("xhs.feed.recommend", "Read recommended public note feed", RiskLevel.SAFE_READ, false),
            new ToolSpec("xhs.note.publish.image", "Publish image note", RiskLevel.HIGH_RISK_WRITE, false),
            new ToolSpec("xhs.note.publish.video", "Publish video note", RiskLevel.HIGH_RISK_WRITE, false),
            new ToolSpec("xhs.comment.create", "Create comment", RiskLevel.HIGH_RISK_WRITE, false),
            new ToolSpec("xhs.comment.reply", "Reply to comment", RiskLevel.HIGH_RISK_WRITE, false),
            new ToolSpec("xhs.note.like", "Like note", RiskLevel.HIGH_RISK_WRITE, false),
            new ToolSpec("xhs.note.collect", "Collect note", RiskLevel.HIGH_RISK_WRITE, false)
    );

    public List<ToolSpec> allTools() {
        return tools;
    }

    public List<ToolSpec> exposedTools() {
        return tools.stream().filter(ToolSpec::agentVisible).toList();
    }

    public List<ToolSpec> disabledTools() {
        return tools.stream().filter(tool -> !tool.agentVisible()).toList();
    }

    public boolean isAgentVisible(String name) {
        String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return tools.stream()
                .anyMatch(tool -> tool.name().equalsIgnoreCase(normalized) && tool.agentVisible());
    }

    public enum RiskLevel {
        SAFE_READ,
        MEDIUM_RISK_READ,
        HIGH_RISK_WRITE
    }

    public record ToolSpec(String name, String description, RiskLevel riskLevel, boolean agentVisible) {
    }
}

package com.flowmind.agent.extension.xiaohongshu;

import com.flowmind.common.core.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agents/xiaohongshu")
public class XiaohongshuMcpController {
    private final XiaohongshuMcpClient client;
    private final XiaohongshuMcpToolRegistry registry;
    private final XiaohongshuKeywordPlanner keywordPlanner;

    public XiaohongshuMcpController(XiaohongshuMcpClient client,
                                    XiaohongshuMcpToolRegistry registry,
                                    XiaohongshuKeywordPlanner keywordPlanner) {
        this.client = client;
        this.registry = registry;
        this.keywordPlanner = keywordPlanner;
    }

    @GetMapping("/tools")
    public ApiResponse<?> tools() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("agentVisible", registry.exposedTools());
        body.put("disabledInternal", registry.disabledTools());
        body.put("note", "Only read/search tools are visible to FlowMind Agent. Publish/comment/like/collect tools are registered but disabled.");
        return ApiResponse.success(body);
    }

    @GetMapping("/search")
    public ApiResponse<?> search(@RequestParam String topic,
                                 @RequestParam(defaultValue = "8") int limit,
                                 @RequestParam(required = false) String sortBy,
                                 @RequestParam(required = false) String noteType,
                                 @RequestParam(required = false) String publishTime,
                                 @RequestParam(required = false) String searchScope,
                                 @RequestParam(required = false) String location) {
        XiaohongshuMcpClient.SearchOptions options =
                new XiaohongshuMcpClient.SearchOptions(sortBy, noteType, publishTime, searchScope, location);
        XiaohongshuMcpClient.SearchResult result = client.searchHotNotes(topic, limit, options);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("topic", result.topic());
        body.put("mode", result.mode());
        body.put("message", result.message());
        body.put("filters", options.toFilterMap());
        body.put("notes", result.notes().stream().map(XiaohongshuMcpClient.HotNote::toMap).toList());
        return ApiResponse.success(body);
    }

    @GetMapping("/search/batch")
    public ApiResponse<?> searchBatch(@RequestParam(defaultValue = "保研") String topic,
                                      @RequestParam(required = false) String topics,
                                      @RequestParam(defaultValue = "10") int limit,
                                      @RequestParam(defaultValue = "8") int perTopicLimit,
                                      @RequestParam(defaultValue = "local") String keywordMode,
                                      @RequestParam(defaultValue = "10") int keywordLimit,
                                      @RequestParam(required = false) String sortBy,
                                      @RequestParam(required = false) String noteType,
                                      @RequestParam(required = false) String publishTime,
                                      @RequestParam(required = false) String searchScope,
                                      @RequestParam(required = false) String location) {
        XiaohongshuMcpClient.SearchOptions options =
                new XiaohongshuMcpClient.SearchOptions(sortBy, noteType, publishTime, searchScope, location);
        XiaohongshuKeywordPlanner.KeywordPlan plan = buildKeywordPlan(topic, topics, keywordMode, keywordLimit);
        XiaohongshuMcpClient.BatchSearchResult result =
                client.searchHotNotesBatch(topic, String.join(",", plan.keywords()), limit, perTopicLimit, options);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("topic", result.topic());
        body.put("keywords", result.keywords());
        body.put("keywordMode", keywordMode);
        body.put("keywordPlan", Map.of(
                "llmUsed", plan.llmUsed(),
                "message", plan.message(),
                "keywords", plan.keywords()
        ));
        body.put("filters", options.toFilterMap());
        body.put("mode", result.mode());
        body.put("message", result.message());
        body.put("traces", result.traces());
        body.put("notes", result.notes().stream().map(XiaohongshuMcpClient.BatchHotNote::toMap).toList());
        return ApiResponse.success(body);
    }

    private XiaohongshuKeywordPlanner.KeywordPlan buildKeywordPlan(String topic, String topics, String keywordMode, int keywordLimit) {
        if (topics != null && !topics.isBlank()) {
            return new XiaohongshuKeywordPlanner.KeywordPlan(false, "manual keyword list", java.util.Arrays.stream(topics.split("[,，\\n\\r]+"))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .limit(Math.max(1, Math.min(keywordLimit, 12)))
                    .toList());
        }
        String mode = keywordMode == null ? "local" : keywordMode.trim().toLowerCase();
        if ("none".equals(mode) || "single".equals(mode)) {
            return new XiaohongshuKeywordPlanner.KeywordPlan(false, "single keyword only", java.util.List.of(topic));
        }
        return keywordPlanner.plan(topic, keywordLimit, "llm".equals(mode) || "ai".equals(mode));
    }

    @GetMapping("/detail")
    public ApiResponse<?> detail(@RequestParam String id) {
        return ApiResponse.success(client.readNoteDetail(id));
    }
}

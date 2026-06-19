package com.flowmind.agent.extension.xiaohongshu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class XiaohongshuMcpClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${flowmind.tools.xiaohongshu-mcp.enabled:false}")
    private boolean enabled;

    @Value("${flowmind.tools.xiaohongshu-mcp.base-url:}")
    private String baseUrl;

    @Value("${flowmind.tools.xiaohongshu-mcp.search-path:/api/xiaohongshu/search}")
    private String searchPath;

    @Value("${flowmind.tools.xiaohongshu-mcp.detail-path:/api/xiaohongshu/detail}")
    private String detailPath;

    @Value("${flowmind.tools.xiaohongshu-mcp.timeout-seconds:20}")
    private int timeoutSeconds;

    @Value("${flowmind.tools.xiaohongshu-mcp.mock-fallback:true}")
    private boolean mockFallback;

    public XiaohongshuMcpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public SearchResult searchHotNotes(String topic, int limit) {
        String safeTopic = normalizeTopic(topic);
        int safeLimit = Math.max(3, Math.min(limit, 20));
        if (!enabled || baseUrl == null || baseUrl.isBlank()) {
            return SearchResult.mock(safeTopic, safeLimit, "xiaohongshu-mcp is not configured; using FlowMind mock hot notes.");
        }
        try {
            SearchResult result = callSearch(safeTopic, safeLimit);
            if (result.notes().isEmpty() && mockFallback) {
                return SearchResult.mock(safeTopic, safeLimit, "xiaohongshu-mcp returned empty data; using FlowMind mock hot notes.");
            }
            return result;
        } catch (Exception e) {
            if (mockFallback) {
                return SearchResult.mock(safeTopic, safeLimit, "xiaohongshu-mcp call failed: " + e.getMessage() + "; using FlowMind mock hot notes.");
            }
            return new SearchResult(safeTopic, List.of(), "failed", e.getMessage());
        }
    }

    public Map<String, Object> readNoteDetail(String noteIdOrUrl) {
        String value = noteIdOrUrl == null ? "" : noteIdOrUrl.trim();
        if (value.isEmpty()) {
            return Map.of("ok", false, "message", "noteIdOrUrl is empty");
        }
        if (!enabled || baseUrl == null || baseUrl.isBlank()) {
            return Map.of("ok", false, "message", "xiaohongshu-mcp is not configured");
        }
        try {
            String url = trimBase(baseUrl) + detailPath + "?id=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(5, timeoutSeconds)))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Map.of("ok", false, "message", "HTTP " + response.statusCode(), "body", response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            return objectMapper.convertValue(root, Map.class);
        } catch (Exception e) {
            return Map.of("ok", false, "message", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private SearchResult callSearch(String topic, int limit) throws IOException, InterruptedException {
        String url = trimBase(baseUrl) + searchPath
                + "?keyword=" + URLEncoder.encode(topic, StandardCharsets.UTF_8)
                + "&limit=" + limit;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(Math.max(5, timeoutSeconds)))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + ": " + trim(response.body(), 500));
        }
        JsonNode root = objectMapper.readTree(response.body());
        List<HotNote> notes = parseNotes(root, limit);
        return new SearchResult(topic, notes, "real", "xiaohongshu-mcp search returned " + notes.size() + " notes.");
    }

    private List<HotNote> parseNotes(JsonNode root, int limit) {
        JsonNode array = firstArray(root, "data", "items", "notes", "result", "list");
        if (array == null && root.isArray()) {
            array = root;
        }
        List<HotNote> notes = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return notes;
        }
        int count = 0;
        for (JsonNode item : array) {
            if (count >= limit) break;
            notes.add(new HotNote(
                    text(item, "id", "noteId", "note_id", "xsecToken"),
                    text(item, "title", "displayTitle", "name"),
                    text(item, "desc", "content", "noteDesc", "summary"),
                    text(item, "author", "nickname", "userName"),
                    text(item, "url", "link", "shareLink"),
                    number(item, "likeCount", "likedCount", "likes"),
                    number(item, "collectCount", "collectedCount", "collects"),
                    number(item, "commentCount", "comments"),
                    tags(item)
            ));
            count++;
        }
        return notes;
    }

    private JsonNode firstArray(JsonNode root, String... names) {
        if (root == null) return null;
        for (String name : names) {
            JsonNode value = root.get(name);
            if (value != null && value.isArray()) return value;
            if (value != null && value.isObject()) {
                JsonNode nested = firstArray(value, names);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private String text(JsonNode item, String... names) {
        for (String name : names) {
            JsonNode value = item.get(name);
            if (value != null && !value.isNull()) {
                String text = value.asText("").trim();
                if (!text.isEmpty()) return text;
            }
        }
        return "";
    }

    private long number(JsonNode item, String... names) {
        for (String name : names) {
            JsonNode value = item.get(name);
            if (value != null && value.isNumber()) return value.asLong();
            if (value != null && value.isTextual()) {
                try {
                    return Long.parseLong(value.asText().replaceAll("[^0-9]", ""));
                } catch (Exception ignored) {
                }
            }
        }
        return 0;
    }

    private List<String> tags(JsonNode item) {
        List<String> tags = new ArrayList<>();
        JsonNode value = item.get("tags");
        if (value != null && value.isArray()) {
            value.forEach(node -> {
                String tag = node.isObject() ? text(node, "name", "tagName", "title") : node.asText("");
                if (!tag.isBlank()) tags.add(tag);
            });
        }
        return tags;
    }

    private String normalizeTopic(String topic) {
        String value = topic == null ? "" : topic.trim();
        return value.isEmpty() ? "保研经验" : trim(value, 80);
    }

    private String trimBase(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private static String trim(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    public record SearchResult(String topic, List<HotNote> notes, String mode, String message) {
        static SearchResult mock(String topic, int limit, String message) {
            List<HotNote> notes = new ArrayList<>();
            notes.add(new HotNote("mock-1", topic + "别再乱准备了", "痛点开头 + 结果承诺 + 3步方法，强调普通本科也能看懂的行动清单。", "保研学姐A", "", 18200, 4310, 680, List.of("保研", "干货", "经验")));
            notes.add(new HotNote("mock-2", "低 GPA 也能补救的" + topic + "策略", "反差开头，用案例讲补强科研、推荐信、院校梯度和材料表达。", "规划师B", "", 9400, 2100, 312, List.of("低GPA", "逆袭", "申请")));
            notes.add(new HotNote("mock-3", topic + "材料清单，一次讲明白", "清单型结构，按时间线拆材料，结尾引导评论领取模板。", "资料库C", "", 7600, 1880, 205, List.of("材料", "清单", "模板")));
            notes.add(new HotNote("mock-4", "导师视角看" + topic + "，这些细节很加分", "导师视角 + 避坑总结，强调表达专业性和证据链。", "科研助教D", "", 6100, 1350, 166, List.of("导师", "避坑", "科研")));
            notes.add(new HotNote("mock-5", topic + "从0到1复盘", "故事型结构，用前后对比制造可信度，适合情绪增强版。", "上岸案例E", "", 5200, 990, 128, List.of("复盘", "故事", "上岸")));
            return new SearchResult(topic, notes.stream().limit(limit).toList(), "mock", message);
        }
    }

    public record HotNote(String id,
                          String title,
                          String summary,
                          String author,
                          String url,
                          long likeCount,
                          long collectCount,
                          long commentCount,
                          List<String> tags) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("title", title);
            map.put("summary", summary);
            map.put("author", author);
            map.put("url", url);
            map.put("likeCount", likeCount);
            map.put("collectCount", collectCount);
            map.put("commentCount", commentCount);
            map.put("tags", tags);
            return map;
        }
    }
}

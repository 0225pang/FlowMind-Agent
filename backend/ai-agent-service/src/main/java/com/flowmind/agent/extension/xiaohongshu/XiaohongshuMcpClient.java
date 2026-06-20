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
            DetailKey detailKey = parseDetailKey(value);
            if (detailKey.feedId().isBlank() || detailKey.xsecToken().isBlank()) {
                return Map.of(
                        "ok", false,
                        "message", "Xiaohongshu detail requires feedId and xsecToken. Use the id returned by search, formatted as feedId|xsecToken."
                );
            }
            String url = trimBase(baseUrl) + detailPath;
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("feed_id", detailKey.feedId());
            body.put("xsec_token", detailKey.xsecToken());
            body.put("load_all_comments", false);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(Math.max(5, timeoutSeconds)))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
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
        JsonNode array = firstArray(root, "data", "feeds", "items", "notes", "result", "list");
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
            JsonNode noteCard = item.path("noteCard");
            JsonNode interactInfo = noteCard.path("interactInfo");
            String feedId = text(item, "id", "feedId", "feed_id", "noteId", "note_id");
            String xsecToken = text(item, "xsecToken", "xsec_token");
            String stableId = xsecToken.isBlank() ? feedId : feedId + "|" + xsecToken;
            String title = firstNonBlank(
                    pathText(item, "noteCard", "displayTitle"),
                    pathText(item, "noteCard", "title"),
                    text(item, "title", "displayTitle", "name")
            );
            String author = firstNonBlank(
                    pathText(item, "noteCard", "user", "nickname"),
                    pathText(item, "noteCard", "user", "nickName"),
                    text(item, "author", "nickname", "userName")
            );
            String summary = firstNonBlank(
                    pathText(item, "noteCard", "desc"),
                    pathText(item, "noteCard", "content"),
                    text(item, "desc", "content", "noteDesc", "summary"),
                    buildStructureHint(title, interactInfo)
            );
            notes.add(new HotNote(
                    stableId,
                    title,
                    summary,
                    author,
                    firstNonBlank(text(item, "url", "link", "shareLink"), buildDetailUrl(feedId, xsecToken)),
                    coverUrl(noteCard),
                    firstNonZero(number(item, "likeCount", "likedCount", "likes"), number(interactInfo, "likedCount")),
                    firstNonZero(number(item, "collectCount", "collectedCount", "collects"), number(interactInfo, "collectedCount")),
                    firstNonZero(number(item, "commentCount", "comments"), number(interactInfo, "commentCount")),
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

    private String pathText(JsonNode item, String... path) {
        JsonNode current = item;
        for (String name : path) {
            if (current == null || current.isMissingNode() || current.isNull()) return "";
            current = current.path(name);
        }
        return current == null || current.isMissingNode() || current.isNull() ? "" : current.asText("").trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private long firstNonZero(long... values) {
        for (long value : values) {
            if (value > 0) return value;
        }
        return 0;
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

    private String coverUrl(JsonNode noteCard) {
        String direct = pathText(noteCard, "cover", "urlDefault");
        if (!direct.isBlank()) return direct;
        direct = pathText(noteCard, "cover", "url");
        if (!direct.isBlank()) return direct;
        JsonNode infoList = noteCard.path("cover").path("infoList");
        if (infoList.isArray() && !infoList.isEmpty()) {
            return pathText(infoList.get(0), "url");
        }
        return "";
    }

    private String buildStructureHint(String title, JsonNode interactInfo) {
        List<String> clues = new ArrayList<>();
        if (title != null && title.matches(".*\\d+.*")) clues.add("数字型标题");
        if (title != null && (title.contains("别") || title.contains("避坑") || title.contains("不要"))) clues.add("焦虑/避坑钩子");
        if (title != null && (title.contains("清单") || title.contains("模板") || title.contains("步骤"))) clues.add("清单型结构");
        long likes = number(interactInfo, "likedCount");
        long collects = number(interactInfo, "collectedCount");
        if (collects > 0 && likes > 0 && collects * 2 >= likes) clues.add("收藏导向强，适合模板/资料领取转化");
        if (clues.isEmpty()) clues.add("根据标题、互动数据和封面进行爆款结构拆解");
        return String.join("；", clues);
    }

    private DetailKey parseDetailKey(String value) {
        String text = value == null ? "" : value.trim();
        if (text.contains("|")) {
            String[] parts = text.split("\\|", 2);
            return new DetailKey(parts[0].trim(), parts.length > 1 ? parts[1].trim() : "");
        }
        if (text.contains(",")) {
            String[] parts = text.split(",", 2);
            return new DetailKey(parts[0].trim(), parts.length > 1 ? parts[1].trim() : "");
        }
        String feedId = queryValue(text, "feed_id", "feedId", "note_id", "noteId", "id");
        String token = queryValue(text, "xsec_token", "xsecToken");
        if (!feedId.isBlank() || !token.isBlank()) {
            return new DetailKey(feedId, token);
        }
        return new DetailKey(text, "");
    }

    private String queryValue(String value, String... names) {
        for (String name : names) {
            String marker = name + "=";
            int start = value.indexOf(marker);
            if (start < 0) continue;
            int from = start + marker.length();
            int end = value.indexOf('&', from);
            return (end < 0 ? value.substring(from) : value.substring(from, end)).trim();
        }
        return "";
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
            notes.add(new HotNote("mock-1", topic + "别再乱准备了", "痛点开头 + 结果承诺 + 3步方法，强调普通本科也能看懂的行动清单。", "保研学姐A", "", "", 18200, 4310, 680, List.of("保研", "干货", "经验")));
            notes.add(new HotNote("mock-2", "低 GPA 也能补救的" + topic + "策略", "反差开头，用案例讲补强科研、推荐信、院校梯度和材料表达。", "规划师B", "", "", 9400, 2100, 312, List.of("低GPA", "逆袭", "申请")));
            notes.add(new HotNote("mock-3", topic + "材料清单，一次讲明白", "清单型结构，按时间线拆材料，结尾引导评论领取模板。", "资料库C", "", "", 7600, 1880, 205, List.of("材料", "清单", "模板")));
            notes.add(new HotNote("mock-4", "导师视角看" + topic + "，这些细节很加分", "导师视角 + 避坑总结，强调表达专业性和证据链。", "科研助教D", "", "", 6100, 1350, 166, List.of("导师", "避坑", "科研")));
            notes.add(new HotNote("mock-5", topic + "从0到1复盘", "故事型结构，用前后对比制造可信度，适合情绪增强版。", "上岸案例E", "", "", 5200, 990, 128, List.of("复盘", "故事", "上岸")));
            return new SearchResult(topic, notes.stream().limit(limit).toList(), "mock", message);
        }
    }

    public record HotNote(String id,
                          String title,
                          String summary,
                          String author,
                          String url,
                          String coverUrl,
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
            map.put("detailUrl", url);
            map.put("coverUrl", coverUrl);
            map.put("likeCount", likeCount);
            map.put("collectCount", collectCount);
            map.put("commentCount", commentCount);
            map.put("tags", tags);
            return map;
        }
    }

    private record DetailKey(String feedId, String xsecToken) {
    }

    private String buildDetailUrl(String feedId, String xsecToken) {
        if (feedId == null || feedId.isBlank()) return "";
        StringBuilder builder = new StringBuilder("https://www.xiaohongshu.com/explore/")
                .append(URLEncoder.encode(feedId, StandardCharsets.UTF_8));
        if (xsecToken != null && !xsecToken.isBlank()) {
            builder.append("?xsec_token=")
                    .append(URLEncoder.encode(xsecToken, StandardCharsets.UTF_8))
                    .append("&xsec_source=pc_feed");
        }
        return builder.toString();
    }
}

package com.flowmind.agent.extension;

import com.flowmind.agent.dto.AgentRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
class CurrentTimeMcpExtension implements McpToolProvider {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss EEEE");

    public String name() {
        return "current-time";
    }

    public String description() {
        return "获取当前日期、时间和时区。仅在用户明确要求时提供。";
    }

    @Override
    public boolean supports(String agentType) {
        return true;
    }

    public String runtimeContext(AgentRequest request) {
        // Only provide time context when the user explicitly asks for it
        String msg = request.getMessage();
        if (msg == null) return "";
        String lower = msg.toLowerCase();
        if (lower.contains("几点了") || lower.contains("时间") || lower.contains("日期")
                || lower.contains("今天几号") || lower.contains("现在什么时间")
                || lower.contains("what time") || lower.contains("current time")
                || lower.contains("今天星期") || lower.contains("今年是")
                || lower.contains("现在几点")) {
            LocalDateTime now = LocalDateTime.now(ZONE);
            return "当前时间：" + now.format(FORMATTER) + "\n时区：Asia/Shanghai";
        }
        return "";
    }
}

@Component
class WebAccessMcpExtension implements McpToolProvider {
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
    private static final Pattern TITLE_PATTERN = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern RESULT_PATTERN = Pattern.compile("class=\"result__a\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    @Value("${flowmind.tools.web.enabled:true}")
    private boolean enabled;

    @Value("${flowmind.tools.web.search-url:https://duckduckgo.com/html/?q=}")
    private String searchUrl;

    public String name() {
        return "web-access";
    }

    public String description() {
        return "访问互联网，支持 URL 摘要和轻量搜索。正式生产可替换为 Bing/Tavily/SerpAPI/MCP Browser。";
    }

    public String runtimeContext(AgentRequest request) {
        if (!enabled || request == null || request.getMessage() == null) return "";
        String message = request.getMessage();
        // Only search when user explicitly requests web access
        if (!needsSearch(message)) return "";
        Matcher urlMatcher = URL_PATTERN.matcher(message);
        if (urlMatcher.find()) {
            return fetchUrl(urlMatcher.group());
        }
        return search(message);
    }

    private boolean needsSearch(String message) {
        String text = message.toLowerCase();
        return text.contains("联网搜")
                || text.contains("搜索一下")
                || text.contains("帮我搜")
                || text.contains("查一下最新");
    }

    private String fetchUrl(String url) {
        try {
            String html = get(url);
            String title = firstMatch(TITLE_PATTERN, html);
            String text = stripHtml(html);
            return "已访问 URL：" + url + "\n标题：" + fallback(title, "未识别") + "\n摘要：" + summarize(text);
        } catch (Exception e) {
            return "尝试访问 URL 失败：" + url + "\n原因：" + e.getMessage();
        }
    }

    private String search(String query) {
        try {
            String html = get(searchUrl + URLEncoder.encode(query, StandardCharsets.UTF_8));
            Matcher matcher = RESULT_PATTERN.matcher(html);
            StringBuilder builder = new StringBuilder("联网搜索关键词：").append(query).append("\n搜索结果摘要：\n");
            int count = 0;
            while (matcher.find() && count < 5) {
                count++;
                builder.append(count).append(". ").append(stripHtml(matcher.group(1))).append("\n");
            }
            if (count == 0) {
                builder.append(summarize(stripHtml(html))).append("\n");
            }
            return builder.toString();
        } catch (Exception e) {
            return "联网搜索失败：" + e.getMessage() + "\n请说明：当前环境可能无法访问外网，或需要配置正式搜索服务。";
        }
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "FlowMindAgent/0.1")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    private String firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? stripHtml(matcher.group(1)) : "";
    }

    private String stripHtml(String html) {
        return html.replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String summarize(String text) {
        if (text == null || text.isBlank()) return "未提取到可读文本。";
        return text.length() <= 600 ? text : text.substring(0, 600) + "...";
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

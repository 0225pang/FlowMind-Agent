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
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEEE");

    @Override
    public String name() {
        return "current-time";
    }

    @Override
    public String description() {
        return "Get the current date, time and timezone. Use only when the user asks about current time/date.";
    }

    @Override
    public boolean supports(String agentType) {
        return true;
    }

    @Override
    public String runtimeContext(AgentRequest request) {
        if (request == null || request.getMessage() == null) return "";
        String lower = request.getMessage().toLowerCase();
        if (!containsAny(lower, "几点", "时间", "日期", "今天几号", "现在是什么时间",
                "今天星期", "今年是", "current time", "what time", "date today")) {
            return "";
        }
        LocalDateTime now = LocalDateTime.now(ZONE);
        return "Current time: " + now.format(FORMATTER) + "\nTimezone: Asia/Shanghai";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) return true;
        }
        return false;
    }
}

@Component
class WebAccessMcpExtension implements McpToolProvider {
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
    private static final Pattern TITLE_PATTERN = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern RESULT_PATTERN = Pattern.compile(
            "<a[^>]+class=\"result__a\"[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${flowmind.tools.web.enabled:true}")
    private boolean enabled;

    @Value("${flowmind.tools.web.search-url:https://duckduckgo.com/html/?q=}")
    private String searchUrl;

    @Override
    public String name() {
        return "web-access";
    }

    @Override
    public String description() {
        return "Lightweight web access. Supports URL title/summary and DuckDuckGo HTML search. Results are search snippets, not verified authoritative facts.";
    }

    @Override
    public boolean supports(String agentType) {
        return true;
    }

    @Override
    public String runtimeContext(AgentRequest request) {
        if (!enabled || request == null || request.getMessage() == null) return "";
        String message = request.getMessage();
        if (!needsSearch(message)) return "";

        Matcher urlMatcher = URL_PATTERN.matcher(message);
        if (urlMatcher.find()) {
            return fetchUrl(urlMatcher.group());
        }
        return search(message);
    }

    private boolean needsSearch(String message) {
        String text = message.toLowerCase();
        return containsAny(text,
                "联网", "上网", "搜索", "查一下", "查一查", "最新", "今天的", "现在的",
                "浏览网页", "打开网页", "访问", "http://", "https://",
                "web search", "search web", "google", "duckduckgo");
    }

    private String fetchUrl(String url) {
        try {
            String html = get(url);
            String title = firstMatch(TITLE_PATTERN, html);
            String text = stripHtml(html);
            return "Web URL fetched.\n"
                    + "Reliability: medium. This is raw webpage text and may need verification.\n"
                    + "URL: " + url + "\n"
                    + "Title: " + fallback(title, "unknown") + "\n"
                    + "Summary: " + summarize(text);
        } catch (Exception e) {
            return "Web URL fetch failed.\nURL: " + url + "\nReason: " + e.getMessage();
        }
    }

    private String search(String query) {
        try {
            String html = get(searchUrl + URLEncoder.encode(query, StandardCharsets.UTF_8));
            Matcher matcher = RESULT_PATTERN.matcher(html);
            StringBuilder builder = new StringBuilder();
            builder.append("Web search was called.\n");
            builder.append("Reliability: low to medium. These are search-result titles/links from DuckDuckGo HTML, not verified source facts. Prefer official or primary sources before making strong claims.\n");
            builder.append("Query: ").append(query).append("\n");
            builder.append("Results:\n");

            int count = 0;
            while (matcher.find() && count < 5) {
                count++;
                String href = stripHtml(matcher.group(1));
                String title = stripHtml(matcher.group(2));
                builder.append(count).append(". ").append(title).append("\n")
                        .append("   ").append(href).append("\n");
            }
            if (count == 0) {
                builder.append("No structured search results parsed. Raw page summary: ")
                        .append(summarize(stripHtml(html))).append("\n");
            }
            return builder.toString();
        } catch (Exception e) {
            return "Web search failed: " + e.getMessage()
                    + "\nThe runtime environment may not have external network access, or a formal search API may need to be configured.";
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
        if (html == null) return "";
        return html.replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String summarize(String text) {
        if (text == null || text.isBlank()) return "No readable text extracted.";
        return text.length() <= 700 ? text : text.substring(0, 700) + "...";
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) return true;
        }
        return false;
    }
}

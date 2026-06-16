package com.flowmind.agent.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmind.agent.dto.AgentRequest;
import com.flowmind.agent.dto.AgentResponse;
import com.flowmind.agent.extension.AgentExtension;
import com.flowmind.agent.llm.LLMClient;
import com.flowmind.agent.service.LarkCliToolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FeishuAgent extends BaseAgent {
    private static final Logger log = LoggerFactory.getLogger(FeishuAgent.class);
    private static final Pattern QUOTED_TITLE = Pattern.compile("[\"“《](.*?)[\"”》]");
    private static final Pattern TOOL_JSON = Pattern.compile("\\{\\s*\"action\"\\s*:\\s*\"CREATE_DOC\"[\\s\\S]*?}");

    private final LarkCliToolService toolService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${flowmind.feishu.knowledge-base.name:保研知识库}")
    private String knowledgeBaseName;

    @Value("${flowmind.feishu.knowledge-base.folder-token:}")
    private String knowledgeBaseFolderToken;

    @Value("${flowmind.feishu.knowledge-base.url:}")
    private String knowledgeBaseUrl;

    public FeishuAgent(LLMClient llm, List<AgentExtension> extensions, LarkCliToolService toolService) {
        super(llm, extensions);
        this.toolService = toolService;
    }

    @Override
    public String getName() {
        return "FeishuAgent";
    }

    @Override
    public String getDescription() {
        return "飞书工具智能体：通过后端白名单调用 lark-cli，支持创建文档、读取文档、读取保研知识库共享文件夹。";
    }

    @Override
    public AgentResponse execute(AgentRequest request) {
        String message = safe(request.getMessage());
        try {
            if (isDocCreateRequest(message)) {
                return createDocResponse(message, extractTitle(message), extractContent(message), inferParentToken(message));
            }
            if (isFolderListRequest(message)) {
                return listKnowledgeFolderResponse();
            }
        } catch (Exception e) {
            log.warn("Feishu tool call failed", e);
            return AgentResponse.of("feishu", failureText(e),
                    List.of(Map.of("title", "Feishu tool failed", "content", e.getMessage())));
        }

        AgentResponse llmResponse = response("feishu", message, List.of(
                Map.of("title", "保研知识库", "content", "已配置默认共享文件夹，可直接问：保研知识库里有哪些文档？"),
                Map.of("title", "创建文档", "content", "可直接说：在保研知识库里创建一个飞书文档，内容是...")
        ));
        return executeToolJsonIfPresent(llmResponse);
    }

    @Override
    public void stream(AgentRequest request, Consumer<String> onDelta) {
        String message = safe(request.getMessage());
        try {
            if (isDocCreateRequest(message)) {
                onDelta.accept("正在调用飞书 CLI 创建文档...\n\n");
                onDelta.accept(createDocResponse(message, extractTitle(message), extractContent(message), inferParentToken(message)).getReply());
                return;
            }
            if (isFolderListRequest(message)) {
                onDelta.accept("正在读取“" + knowledgeBaseName + "”共享文件夹...\n\n");
                onDelta.accept(listKnowledgeFolderResponse().getReply());
                return;
            }
        } catch (Exception e) {
            log.warn("Feishu tool call failed", e);
            onDelta.accept(failureText(e));
            return;
        }
        super.stream(request, onDelta);
    }

    private AgentResponse listKnowledgeFolderResponse() throws Exception {
        if (knowledgeBaseFolderToken == null || knowledgeBaseFolderToken.isBlank()) {
            throw new IllegalStateException("Knowledge folder token is not configured.");
        }
        JsonNode result = toolService.listFolder(knowledgeBaseFolderToken, "user");
        JsonNode files = result.path("data").path("files");

        StringBuilder reply = new StringBuilder();
        reply.append("✅ 已连接并读取“").append(knowledgeBaseName).append("”共享文件夹。\n\n");
        reply.append("- Folder Token：`").append(knowledgeBaseFolderToken).append("`\n");
        if (knowledgeBaseUrl != null && !knowledgeBaseUrl.isBlank()) {
            reply.append("- 文件夹链接：").append(knowledgeBaseUrl).append("\n");
        }
        reply.append("\n");

        List<Map<String, Object>> cards = new ArrayList<>();
        if (!files.isArray() || files.isEmpty()) {
            reply.append("当前文件夹没有读取到直接子项。");
        } else {
            reply.append("当前直接子项：\n\n");
            int index = 1;
            for (JsonNode file : files) {
                String name = file.path("name").asText("未命名");
                String type = file.path("type").asText("");
                String token = file.path("token").asText("");
                String url = file.path("url").asText("");
                reply.append(index++).append(". ").append(name);
                if (!type.isBlank()) reply.append("（").append(type).append("）");
                reply.append("\n");
                if (!url.isBlank()) reply.append("   ").append(url).append("\n");
                cards.add(Map.of("title", name, "type", type, "token", token, "url", url));
            }
        }
        return AgentResponse.of("feishu", reply.toString(), cards);
    }

    private AgentResponse executeToolJsonIfPresent(AgentResponse response) {
        String reply = safe(response.getReply());
        Matcher matcher = TOOL_JSON.matcher(reply);
        if (!matcher.find()) {
            return response;
        }
        try {
            JsonNode payload = objectMapper.readTree(matcher.group());
            String title = payload.path("title").asText("FlowMind 飞书文档");
            String content = payload.path("content").asText("");
            String parentToken = payload.path("parentToken").asText("");
            return createDocResponse(reply, title, content, parentToken);
        } catch (Exception e) {
            log.warn("Failed to execute CREATE_DOC tool JSON", e);
            response.setReply(failureText(e));
            return response;
        }
    }

    private AgentResponse createDocResponse(String sourceMessage, String title, String content, String parentToken) throws Exception {
        if (!toolService.isAvailable()) {
            throw new IllegalStateException("lark-cli is unavailable. Please run `lark-cli --version` and check backend PATH.");
        }
        JsonNode result = toolService.createDoc(title, content, parentToken, "user", ignored -> {});
        String url = extract(result, "data.document.url", "data.url", "url");
        String documentId = extract(result, "data.document.document_id", "data.document_id", "document_id");

        StringBuilder reply = new StringBuilder();
        reply.append("✅ 飞书文档创建成功。\n\n");
        reply.append("- 标题：").append(title).append("\n");
        if (parentToken != null && !parentToken.isBlank()) {
            reply.append("- 创建位置：").append(knowledgeBaseName).append("（`").append(parentToken).append("`）\n");
        }
        if (!url.isBlank()) reply.append("- 链接：").append(url).append("\n");
        if (!documentId.isBlank()) reply.append("- Document ID：`").append(documentId).append("`\n");
        reply.append("\n该结果来自后端白名单工具 `lark-cli docs +create`，不是模拟链接。");

        return AgentResponse.of("feishu", reply.toString(), List.of(
                Map.of("title", title, "content", content, "url", url, "documentId", documentId, "sourceMessage", sourceMessage)
        ));
    }

    private boolean isFolderListRequest(String message) {
        String text = safe(message).toLowerCase();
        if (isDocCreateRequest(message)) {
            return false;
        }
        boolean mentionsFolder = containsAny(text, knowledgeBaseName, "保研知识库", "共享文件夹", "文件夹");
        boolean listIntent = containsAny(text, "访问", "读取", "查看", "列出", "有什么", "有哪些", "里面", "目录", "文件", "清单");
        return mentionsFolder && listIntent;
    }

    private boolean isDocCreateRequest(String message) {
        String text = safe(message).toLowerCase();
        boolean mentionsDoc = containsAny(text, "飞书", "lark", "feishu", "文档", "doc", "云文档", knowledgeBaseName);
        boolean createIntent = containsAny(text, "创建", "新建", "写一个", "写一篇", "写一份", "写上", "生成", "create");
        return mentionsDoc && createIntent;
    }

    private String inferParentToken(String message) {
        if (containsAny(safe(message), knowledgeBaseName, "保研知识库", "共享文件夹")) {
            return safe(knowledgeBaseFolderToken);
        }
        return "";
    }

    private String extractTitle(String message) {
        Matcher matcher = QUOTED_TITLE.matcher(message);
        if (matcher.find() && !matcher.group(1).isBlank()) {
            return cleanTitle(matcher.group(1));
        }
        String content = extractContent(message);
        if (!content.isBlank() && content.length() <= 30) {
            return cleanTitle(content);
        }
        String text = message
                .replaceAll("(?i)flowmind", "")
                .replace("请", "")
                .replace("帮我", "")
                .replace("给我", "")
                .replace("创建", "")
                .replace("新建", "")
                .replace("生成", "")
                .replace("写一个", "")
                .replace("写一篇", "")
                .replace("写一份", "")
                .replace("飞书", "")
                .replace("文档", "")
                .replace("里面", "")
                .replace("写上", "")
                .trim();
        if (text.length() > 30) text = text.substring(0, 30);
        return cleanTitle(text.isBlank() ? "FlowMind 飞书文档" : text);
    }

    private String extractContent(String message) {
        String[] markers = {"里面写上", "内容是", "写上", "记录一下", "写：", "写:"};
        for (String marker : markers) {
            int index = message.indexOf(marker);
            if (index >= 0) {
                String content = message.substring(index + marker.length()).trim();
                return content.isBlank() ? message : content;
            }
        }
        return message.trim();
    }

    private String cleanTitle(String title) {
        String cleaned = safe(title).replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.length() > 60) cleaned = cleaned.substring(0, 60);
        return cleaned.isBlank() ? "FlowMind 飞书文档" : cleaned;
    }

    private String failureText(Exception e) {
        return "❌ 飞书工具调用失败："
                + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())
                + "\n\n请确认后端启动环境能执行 `lark-cli --version`，并且当前用户已授权 `space:document:retrieve`。";
    }

    private static String extract(JsonNode node, String... paths) {
        for (String path : paths) {
            JsonNode current = node;
            for (String segment : path.split("\\.")) current = current.path(segment);
            String value = current.asText("");
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private boolean containsAny(String text, String... keywords) {
        String safeText = safe(text).toLowerCase();
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && safeText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

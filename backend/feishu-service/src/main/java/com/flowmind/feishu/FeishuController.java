package com.flowmind.feishu;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowmind.agent.service.LarkCliToolService;
import com.flowmind.common.core.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/feishu")
public class FeishuController {
    private final LarkCliToolService toolService;
    private final FeishuMockClient mockClient;
    private final List<Map<String, Object>> logs = new CopyOnWriteArrayList<>();

    @Value("${flowmind.feishu.knowledge-base.folder-token:}")
    private String knowledgeBaseFolderToken;

    @Value("${flowmind.feishu.knowledge-base.name:保研知识库}")
    private String knowledgeBaseName;

    public FeishuController(FeishuMockClient mockClient, LarkCliToolService toolService) {
        this.mockClient = mockClient;
        this.toolService = toolService;
        for (int i = 1; i <= 10; i++) {
            logs.add(Map.of(
                    "id", i,
                    "type", i % 2 == 0 ? "bitable" : "docs",
                    "status", "SUCCESS",
                    "message", "第 " + i + " 次 Mock 同步记录",
                    "createdAt", LocalDateTime.now().minusHours(i).toString()
            ));
        }
    }

    @GetMapping("/sync/status")
    public ApiResponse<?> status() {
        boolean larkAvailable = toolService.isAvailable();
        return ApiResponse.success(Map.of(
                "docs", larkAvailable ? "CONNECTED" : "MOCK",
                "bitable", "WAITING",
                "tasks", "WAITING",
                "bot", "READY",
                "larkCli", larkAvailable ? "AVAILABLE" : "UNAVAILABLE"
        ));
    }

    // ---- 真实飞书文档操作 ----

    @PostMapping("/docs/create")
    public ApiResponse<?> createDoc(@RequestBody Map<String, Object> body) {
        try {
            String title = String.valueOf(body.getOrDefault("title", "未命名文档"));
            String content = String.valueOf(body.getOrDefault("content", ""));
            String parentToken = String.valueOf(body.getOrDefault("parentToken", ""));
            String as = String.valueOf(body.getOrDefault("as", "user"));

            JsonNode result = toolService.createDoc(title, content, parentToken, as,
                    progress -> { /* no streaming in REST API */ });
            logs.add(0, Map.of(
                    "id", System.currentTimeMillis(),
                    "type", "docs_create",
                    "status", "SUCCESS",
                    "message", "飞书文档创建: " + title,
                    "createdAt", LocalDateTime.now().toString(),
                    "result", result.toString()
            ));
            return ApiResponse.success(result);
        } catch (Exception e) {
            logs.add(0, Map.of(
                    "id", System.currentTimeMillis(),
                    "type", "docs_create",
                    "status", "FAILED",
                    "message", e.getMessage(),
                    "createdAt", LocalDateTime.now().toString()
            ));
            return ApiResponse.failed(e.getMessage());
        }
    }

    @PostMapping("/docs/fetch")
    public ApiResponse<?> fetchDoc(@RequestBody Map<String, Object> body) {
        try {
            String docToken = String.valueOf(body.get("docToken"));
            String as = String.valueOf(body.getOrDefault("as", "user"));
            JsonNode result = toolService.fetchDoc(docToken, as);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.failed(e.getMessage());
        }
    }

    @GetMapping("/lark-cli/status")
    public ApiResponse<?> larkCliStatus() {
        try {
            String version = toolService.checkVersion();
            return ApiResponse.success(Map.of(
                    "available", true,
                    "version", version
            ));
        } catch (Exception e) {
            return ApiResponse.success(Map.of(
                    "available", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/knowledge-base/files")
    public ApiResponse<?> knowledgeBaseFiles() {
        try {
            JsonNode result = toolService.listFolder(knowledgeBaseFolderToken, "user");
            return ApiResponse.success(Map.of(
                    "name", knowledgeBaseName,
                    "folderToken", knowledgeBaseFolderToken,
                    "result", result
            ));
        } catch (Exception e) {
            return ApiResponse.failed(e.getMessage());
        }
    }

    // ---- 兼容旧 Mock 端点 ----

    @PostMapping("/sync/docs")
    public ApiResponse<?> syncDocs() {
        return sync("docs");
    }

    @PostMapping("/sync/bitable")
    public ApiResponse<?> bitable() {
        return sync("bitable");
    }

    @PostMapping("/sync/tasks")
    public ApiResponse<?> tasks() {
        return sync("tasks");
    }

    @PostMapping("/bot/push")
    public ApiResponse<?> push(@RequestBody Map<String, Object> body) {
        return sync("bot_push");
    }

    @GetMapping("/logs")
    public ApiResponse<?> getLogs() {
        return ApiResponse.success(logs);
    }

    private ApiResponse<?> sync(String type) {
        Map<String, Object> r = mockClient.sync(type);
        logs.add(0, r);
        return ApiResponse.success(r);
    }
}

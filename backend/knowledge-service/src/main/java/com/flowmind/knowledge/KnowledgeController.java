package com.flowmind.knowledge;

import com.flowmind.common.core.ApiResponse;
import com.flowmind.knowledge.entity.KnowledgeDocEntity;
import com.flowmind.knowledge.entity.SyncLogEntity;
import com.flowmind.knowledge.mapper.SyncLogMapper;
import com.flowmind.knowledge.service.KnowledgeService;
import com.flowmind.knowledge.vector.EmbeddingService;
import com.flowmind.knowledge.vector.WeaviateClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeController.class);
    private static final ZoneId BJ = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final KnowledgeService service;
    private final SyncLogMapper syncLogMapper;
    private final EmbeddingService embeddingService;
    private final WeaviateClientService weaviateClient;

    public KnowledgeController(KnowledgeService service, SyncLogMapper syncLogMapper,
                               EmbeddingService embeddingService, WeaviateClientService weaviateClient) {
        this.service = service;
        this.syncLogMapper = syncLogMapper;
        this.embeddingService = embeddingService;
        this.weaviateClient = weaviateClient;
    }

    // ── Search & List ──

    @GetMapping("/docs")
    public ApiResponse<?> searchDocs(@RequestParam(required = false, defaultValue = "") String keyword) {
        List<KnowledgeDocEntity> docs = service.searchDocs(keyword);
        List<Map<String, Object>> result = docs.stream().map(this::toMap).toList();
        return ApiResponse.success(result);
    }

    @GetMapping("/docs/{id}")
    public ApiResponse<?> getDoc(@PathVariable Long id) {
        return service.getDoc(id)
                .map(doc -> ApiResponse.success(toMap(doc)))
                .orElse(ApiResponse.failed("文档不存在: " + id));
    }

    // ── Tags ──

    @PutMapping("/docs/{id}/tags")
    public ApiResponse<?> updateTags(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) body.getOrDefault("tags", List.of());
        KnowledgeDocEntity updated = service.updateTags(id, tags);
        return ApiResponse.success(toMap(updated));
    }

    // ── Sync ──

    @PostMapping("/sync")
    public ApiResponse<?> syncFromFeishu() {
        log.info("Starting Feishu sync...");
        Map<String, Object> result = service.syncFromFeishu();
        return ApiResponse.success(result);
    }

    // ── Sync logs ──

    @GetMapping("/sync-logs")
    public ApiResponse<?> syncLogs() {
        List<SyncLogEntity> logs = syncLogMapper.listRecent(50);
        List<Map<String, Object>> result = logs.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("syncType", l.getSyncType());
            m.put("status", l.getStatus());
            m.put("message", l.getMessage());
            m.put("added", l.getAdded());
            m.put("updated", l.getUpdated());
            m.put("skipped", l.getSkipped());
            m.put("errors", l.getErrors());
            m.put("createdAt", fmtBj(l.getCreatedAt()));
            return m;
        }).toList();
        return ApiResponse.success(result);
    }

    // ── Sync status ──

    @GetMapping("/sync-status")
    public ApiResponse<?> syncStatus() {
        SyncLogEntity docsLog = syncLogMapper.latestByType("docs");
        SyncLogEntity bitableLog = syncLogMapper.latestByType("bitable");
        SyncLogEntity tasksLog = syncLogMapper.latestByType("tasks");
        SyncLogEntity botLog = syncLogMapper.latestByType("bot");

        return ApiResponse.success(Map.of(
                "docs", statusMap(docsLog, 10),
                "bitable", statusMap(bitableLog, 0),
                "tasks", statusMap(tasksLog, 0),
                "bot", statusMap(botLog, 0)
        ));
    }

    private Map<String, Object> statusMap(SyncLogEntity log, int fallbackCount) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (log != null) {
            m.put("status", log.getStatus());
            m.put("lastSync", fmtBj(log.getCreatedAt()));
            m.put("added", log.getAdded());
            m.put("updated", log.getUpdated());
            m.put("skipped", log.getSkipped());
            m.put("errors", log.getErrors());
            m.put("count", log.getAdded() + log.getUpdated());
        } else {
            m.put("status", "--");
            m.put("lastSync", null);
            m.put("added", 0);
            m.put("updated", 0);
            m.put("skipped", 0);
            m.put("errors", 0);
            m.put("count", fallbackCount);
        }
        return m;
    }

    // ── Stats ──

    @GetMapping("/stats")
    public ApiResponse<?> stats() {
        return ApiResponse.success(Map.of(
                "docCount", service.docCount()
        ));
    }

    // ── Tags ──

    @GetMapping("/tags")
    public ApiResponse<?> tags() {
        Set<String> tagSet = new LinkedHashSet<>();
        for (KnowledgeDocEntity doc : service.searchDocs("")) {
            List<String> docTags = parseTags(doc.getTags());
            tagSet.addAll(docTags);
        }
        return ApiResponse.success(new ArrayList<>(tagSet));
    }

    // ── Vector / Semantic Search ──

    @GetMapping("/search")
    public ApiResponse<?> semanticSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int topK) {
        if (!weaviateClient.isEnabled()) {
            return ApiResponse.failed("Weaviate not enabled");
        }
        try {
            float[] queryVec = embeddingService.embed(q);
            if (queryVec.length == 0) {
                return ApiResponse.failed("Embedding failed");
            }
            List<WeaviateClientService.SearchHit> hits = weaviateClient.search(queryVec, topK);
            if (hits.isEmpty()) {
                return ApiResponse.success(List.of(Map.of("message", "No results found")));
            }

            // Deduplicate by feishuToken, keep best distance per doc
            Map<String, WeaviateClientService.SearchHit> best = new LinkedHashMap<>();
            for (WeaviateClientService.SearchHit h : hits) {
                best.merge(h.feishuToken(), h,
                        (a, b) -> a.distance() < b.distance() ? a : b);
            }

            return ApiResponse.success(best.values().stream()
                    .map(h -> Map.of(
                            "mysqlId", h.mysqlId(),
                            "feishuToken", h.feishuToken(),
                            "title", h.title(),
                            "chunkText", h.chunkText(),
                            "feishuUrl", h.feishuUrl(),
                            "feishuType", h.feishuType(),
                            "distance", h.distance()
                    ))
                    .toList());
        } catch (Exception e) {
            log.error("Semantic search failed", e);
            return ApiResponse.failed("Search failed: " + e.getMessage());
        }
    }

    // ── helpers ──

    /** Format LocalDateTime as Beijing-time string for JSON output */
    private static String fmtBj(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(BJ)
                .toLocalDateTime()
                .format(FMT);
    }

    private Map<String, Object> toMap(KnowledgeDocEntity doc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", doc.getId());
        m.put("feishuToken", doc.getFeishuToken());
        m.put("title", doc.getTitle());
        m.put("content", doc.getContent());
        m.put("summary", doc.getSummary());
        m.put("tags", parseTags(doc.getTags()));
        m.put("feishuUrl", doc.getFeishuUrl());
        m.put("feishuType", doc.getFeishuType());
        m.put("createdAt", fmtBj(doc.getCreatedAt()));
        m.put("updatedAt", fmtBj(doc.getUpdatedAt()));
        return m;
    }

    private List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank() || "[]".equals(raw.trim())) return List.of();
        try {
            String inner = raw.trim();
            if (inner.startsWith("[")) inner = inner.substring(1);
            if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
            if (inner.isBlank()) return List.of();
            return Arrays.stream(inner.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.startsWith("\"") ? s.substring(1, s.length() - 1) : s)
                    .toList();
        } catch (Exception e) {
            return List.of(raw);
        }
    }
}

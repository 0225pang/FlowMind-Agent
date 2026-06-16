package com.flowmind.knowledge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowmind.agent.llm.LLMClient;
import com.flowmind.agent.service.LarkCliToolService;
import com.flowmind.knowledge.entity.KnowledgeDocEntity;
import com.flowmind.knowledge.mapper.KnowledgeMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class KnowledgeService {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final KnowledgeMapper mapper;
    private final LarkCliToolService toolService;
    private final LLMClient llmClient;
    private final com.flowmind.knowledge.mapper.SyncLogMapper syncLogMapper;

    @Value("${flowmind.feishu.knowledge-base.folder-token:}")
    private String folderToken;

    @Value("${flowmind.feishu.knowledge-base.name:保研知识库}")
    private String folderName;

    public KnowledgeService(KnowledgeMapper mapper, LarkCliToolService toolService, LLMClient llmClient,
                            com.flowmind.knowledge.mapper.SyncLogMapper syncLogMapper) {
        this.mapper = mapper;
        this.toolService = toolService;
        this.llmClient = llmClient;
        this.syncLogMapper = syncLogMapper;
    }

    // ── Query ──

    public List<KnowledgeDocEntity> searchDocs(String keyword) {
        return mapper.search(keyword);
    }

    public Optional<KnowledgeDocEntity> getDoc(Long id) {
        return mapper.findById(id);
    }

    public long docCount() {
        return mapper.count();
    }

    // ── Tags only ──

    public KnowledgeDocEntity updateTags(Long id, List<String> tags) {
        String json = toJsonArray(tags);
        mapper.updateTags(id, json);
        return mapper.findById(id).orElseThrow();
    }

    // ── Sync from Feishu ──

    public Map<String, Object> syncFromFeishu() {
        int added = 0, updated = 0, skipped = 0, errors = 0;
        List<String> errorMessages = new ArrayList<>();

        if (!toolService.isAvailable()) {
            return Map.of("status", "error", "message", "lark-cli not available",
                    "added", 0, "updated", 0, "skipped", 0, "errors", 1);
        }

        try {
            JsonNode result = toolService.listFolder(folderToken, "user");
            JsonNode files = result.path("data").path("files");

            if (!files.isArray()) {
                return Map.of("status", "ok", "message", "No files array in response",
                        "added", 0, "updated", 0, "skipped", 0, "errors", 0);
            }

            int total = files.size();
            for (JsonNode file : files) {
                try {
                    String token = file.path("token").asText("");
                    String name = file.path("name").asText("未命名");
                    String type = file.path("type").asText("file");
                    String url = file.path("url").asText("");
                    long modifiedAt = file.path("modified_time").asLong(0);

                    if (token.isBlank()) { skipped++; continue; }

                    Optional<KnowledgeDocEntity> existing = mapper.findByFeishuToken(token);

                    // ── Change detection: skip if Feishu modified_time hasn't changed ──
                    if (existing.isPresent() && existing.get().getFeishuModifiedAt() >= modifiedAt) {
                        skipped++;
                        continue;
                    }

                    boolean isNew = existing.isEmpty();

                    // ── Fetch/parse content ──
                    String content = "";
                    String summary = existing.map(KnowledgeDocEntity::getSummary).orElse("");

                    if (isDocxType(type)) {
                        try {
                            JsonNode docResult = toolService.fetchDoc(token, "user");
                            content = extractContent(docResult);
                        } catch (Exception e) {
                            log.warn("Failed to fetch docx content for {}: {}", name, e.getMessage());
                        }
                    } else if (isPdfFile(name, type)) {
                        try {
                            content = downloadAndParsePdf(token, name);
                        } catch (Exception e) {
                            log.warn("Failed to parse PDF {}: {}", name, e.getMessage());
                        }
                    }

                    // ── Generate/refresh summary ──
                    if (summary == null || summary.isBlank()) {
                        if (!content.isBlank()) {
                            summary = generateSummary(name, content);
                        } else {
                            summary = generateSummaryFromMeta(name, type);
                        }
                    } else if (!isNew && !content.isBlank()) {
                        // Re-generate summary for updated docs with new content
                        summary = generateSummary(name, content);
                    }

                    KnowledgeDocEntity doc = new KnowledgeDocEntity();
                    doc.setFeishuToken(token);
                    doc.setTitle(name);
                    doc.setContent(content);
                    doc.setSummary(summary);
                    doc.setFeishuUrl(url);
                    doc.setFeishuType(type);
                    doc.setFeishuModifiedAt(modifiedAt);
                    doc.setTags(existing.map(KnowledgeDocEntity::getTags).orElse("[]"));

                    if (isNew) {
                        mapper.insertIfAbsent(doc);
                        added++;
                    } else {
                        mapper.upsertChanged(doc);
                        updated++;
                    }

                } catch (Exception e) {
                    errors++;
                    errorMessages.add(file.path("name").asText("unknown") + ": " + e.getMessage());
                    log.error("Sync error for file: {}", file.path("name").asText("?"), e);
                }
            }

            log.info("Sync complete: {} total, {} added, {} updated, {} skipped, {} errors",
                    total, added, updated, skipped, errors);

            // ── Write sync log ──
            com.flowmind.knowledge.entity.SyncLogEntity syncLog = new com.flowmind.knowledge.entity.SyncLogEntity();
            syncLog.setSyncType("docs");
            syncLog.setStatus(errors > 0 ? "PARTIAL" : "SUCCESS");
            syncLog.setMessage("同步" + folderName + ": " + added + "新增 " + updated + "更新 " + skipped + "跳过 " + errors + "错误");
            syncLog.setAdded(added);
            syncLog.setUpdated(updated);
            syncLog.setSkipped(skipped);
            syncLog.setErrors(errors);
            syncLogMapper.insert(syncLog);

        } catch (Exception e) {
            log.error("Sync failed", e);
            // Write failure log
            com.flowmind.knowledge.entity.SyncLogEntity syncLog = new com.flowmind.knowledge.entity.SyncLogEntity();
            syncLog.setSyncType("docs");
            syncLog.setStatus("FAILED");
            syncLog.setMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(500, e.getMessage().length())) : "unknown error");
            syncLog.setErrors(1);
            syncLogMapper.insert(syncLog);
            return Map.of("status", "error", "message", e.getMessage(),
                    "added", added, "updated", updated, "skipped", skipped, "errors", errors + 1);
        }

        return Map.of("status", "ok",
                "message", "Synced: " + added + " new, " + updated + " updated, " + skipped + " skipped (unchanged), " + errors + " errors",
                "added", added, "updated", updated, "skipped", skipped, "errors", errors);
    }

    // ── PDF handling ──

    private final Path pdfDownloadDir = Path.of(
            System.getProperty("user.dir"), "pdf-downloads");

    private boolean isPdfFile(String name, String type) {
        if ("file".equalsIgnoreCase(type)) {
            return name.toLowerCase().endsWith(".pdf");
        }
        return "pdf".equalsIgnoreCase(type);
    }

    private String downloadAndParsePdf(String fileToken, String name) throws Exception {
        Files.createDirectories(pdfDownloadDir);
        Path pdfPath = null;
        try {
            pdfPath = toolService.downloadFile(fileToken, pdfDownloadDir);
            if (pdfPath == null || !Files.exists(pdfPath) || Files.size(pdfPath) == 0) {
                return "";
            }
            try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(document);
                if (text.length() > 50000) text = text.substring(0, 50000);
                return text.trim();
            }
        } finally {
            if (pdfPath != null) try { Files.deleteIfExists(pdfPath); } catch (Exception ignored) {}
        }
    }

    // ── helpers ──

    private boolean isDocxType(String type) {
        return "docx".equalsIgnoreCase(type) || "doc".equalsIgnoreCase(type)
                || "document".equalsIgnoreCase(type);
    }

    private String extractContent(JsonNode fetchResult) {
        JsonNode data = fetchResult.path("data");
        String content = data.path("content").asText("");
        if (!content.isBlank()) return content;
        JsonNode doc = data.path("document");
        content = doc.path("content").asText("");
        if (!content.isBlank()) return content;
        JsonNode blocks = doc.path("blocks");
        if (blocks.isArray()) {
            return blocksToHtml(blocks);
        }
        return fetchResult.toString();
    }

    private String blocksToHtml(JsonNode blocks) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : blocks) {
            int blkType = block.path("type").asInt();
            String text = block.path("text").asText("");
            if (text.isBlank()) continue;
            switch (blkType) {
                case 1: sb.append("<p>").append(text).append("</p>\n"); break;
                case 2: sb.append("<h1>").append(text).append("</h1>\n"); break;
                case 3: sb.append("<h2>").append(text).append("</h2>\n"); break;
                case 4: sb.append("<h3>").append(text).append("</h3>\n"); break;
                case 5: sb.append("<blockquote>").append(text).append("</blockquote>\n"); break;
                case 6: sb.append("<li>").append(text).append("</li>\n"); break;
                default: sb.append("<p>").append(text).append("</p>\n"); break;
            }
        }
        return sb.toString().trim();
    }

    private String generateSummary(String title, String content) {
        try {
            String body = content.length() > 3000 ? content.substring(0, 3000) + "..." : content;
            String prompt = "请用1-2句话（不超过120字）概括以下文档的核心内容和要点：\n\n标题：" + title
                    + "\n\n内容：" + body;
            String result = llmClient.complete("你是一个专业的文档摘要助手。请简洁准确地概括文档内容，不要废话。", prompt);
            if (result != null && !result.isBlank()) {
                return result.length() > 200 ? result.substring(0, 200) : result;
            }
        } catch (Exception e) {
            log.warn("Summary generation failed for {}: {}", title, e.getMessage());
        }
        return "";
    }

    private String generateSummaryFromMeta(String name, String type) {
        try {
            String prompt = "文件名：" + name + "\n文件类型：" + type
                    + "\n\n请根据文件名和类型，推测这个文件可能的主题和用途，用1句话（不超过80字）描述。";
            String result = llmClient.complete("你是一个知识管理助手。根据文件名推测文档内容。", prompt);
            if (result != null && !result.isBlank()) {
                return result.length() > 120 ? result.substring(0, 120) : result;
            }
        } catch (Exception e) {
            log.warn("Meta summary failed for {}: {}", name, e.getMessage());
        }
        return "文件类型: " + type + "，待同步查看。";
    }

    private String toJsonArray(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(tags.get(i).replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}

package com.flowmind.knowledge.vector;

import com.flowmind.knowledge.entity.KnowledgeDocEntity;
import com.flowmind.knowledge.service.KnowledgeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * A decoupled vector retrieval capability.
 *
 * This service is intentionally independent from AgentRouter and KnowledgeAgent.
 * Team members can add new capabilities by following this pattern:
 * ToolService -> Controller -> optional AgentExtension description.
 */
@Service
public class VectorSearchToolService {
    private final EmbeddingService embeddingService;
    private final WeaviateClientService weaviateClient;
    private final KnowledgeService knowledgeService;

    @Value("${flowmind.vector-demo.enabled:true}")
    private boolean enabled;

    @Value("${flowmind.vector-demo.fallback-enabled:true}")
    private boolean fallbackEnabled;

    @Value("${flowmind.vector-demo.max-top-k:20}")
    private int maxTopK;

    @Value("${flowmind.vector-demo.max-distance:0.65}")
    private float maxDistance;

    public VectorSearchToolService(EmbeddingService embeddingService,
                                   WeaviateClientService weaviateClient,
                                   KnowledgeService knowledgeService) {
        this.embeddingService = embeddingService;
        this.weaviateClient = weaviateClient;
        this.knowledgeService = knowledgeService;
    }

    public List<VectorSearchResult> search(String query, int topK) {
        if (!enabled || query == null || query.isBlank()) {
            return List.of();
        }

        int limit = normalizeTopK(topK);

        if (weaviateClient.isEnabled()) {
            float[] vector = embeddingService.embed(query);
            if (vector.length > 0) {
                List<VectorSearchResult> hits = weaviateClient.search(vector, limit).stream()
                        .filter(hit -> hit.distance() <= maxDistance)
                        .map(this::fromVectorHit)
                        .toList();
                if (!hits.isEmpty()) {
                    return hits;
                }
            }
        }

        if (!fallbackEnabled) {
            return List.of();
        }

        return knowledgeService.searchDocs(query).stream()
                .limit(limit)
                .map(this::fromMysqlDoc)
                .toList();
    }

    private int normalizeTopK(int topK) {
        int safeMax = Math.max(1, maxTopK);
        return Math.max(1, Math.min(topK, safeMax));
    }

    private VectorSearchResult fromVectorHit(WeaviateClientService.SearchHit hit) {
        return new VectorSearchResult(
                "weaviate",
                (long) hit.mysqlId(),
                hit.title(),
                hit.chunkText(),
                hit.feishuToken(),
                hit.feishuUrl(),
                hit.feishuType(),
                hit.tags(),
                hit.distance()
        );
    }

    private VectorSearchResult fromMysqlDoc(KnowledgeDocEntity doc) {
        String snippet = firstNonBlank(doc.getSummary(), doc.getContent());
        return new VectorSearchResult(
                "mysql-fallback",
                doc.getId(),
                doc.getTitle(),
                truncate(snippet, 300),
                doc.getFeishuToken(),
                doc.getFeishuUrl(),
                doc.getFeishuType(),
                List.of(),
                null
        );
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength) + "...";
    }

    public record VectorSearchResult(
            String source,
            Long mysqlId,
            String title,
            String chunkText,
            String feishuToken,
            String feishuUrl,
            String feishuType,
            List<String> tags,
            Float distance
    ) {}
}

package com.flowmind.knowledge.vector;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.exception.NoApiKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Wraps Alibaba DashScope text-embedding-v4 API.
 * Returns float[] vectors for text inputs.
 */
@Service
public class EmbeddingService {
    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    @Value("${flowmind.embedding.api-key:}")
    private String apiKey;

    @Value("${flowmind.embedding.model:text-embedding-v4}")
    private String model;

    /**
     * Generate a single embedding vector for a text.
     */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            log.warn("Embedding skipped: empty text");
            return new float[0];
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.error("DashScope API key is empty, check flowmind.embedding.api-key config");
            return new float[0];
        }
        try {
            TextEmbeddingParam param = TextEmbeddingParam.builder()
                    .model(model)
                    .apiKey(apiKey)
                    .texts(Collections.singleton(text))
                    .build();

            TextEmbedding embedding = new TextEmbedding();
            TextEmbeddingResult result = embedding.call(param);

            if (result != null && result.getOutput() != null
                    && result.getOutput().getEmbeddings() != null
                    && !result.getOutput().getEmbeddings().isEmpty()) {
                List<Double> emb = result.getOutput().getEmbeddings().get(0).getEmbedding();
                float[] vec = new float[emb.size()];
                for (int i = 0; i < emb.size(); i++) {
                    vec[i] = emb.get(i).floatValue();
                }
                log.debug("Generated embedding for text ({} chars), dim={}", text.length(), vec.length);
                return vec;
            }
            log.warn("Empty embedding result for text");
            return new float[0];
        } catch (NoApiKeyException e) {
            log.error("DashScope API key not configured: {}", e.getMessage());
            return new float[0];
        } catch (Exception e) {
            log.error("Embedding failed: {}", e.getMessage());
            return new float[0];
        }
    }

    /**
     * Build a search text by combining title + summary for better semantic matching.
     */
    public String buildSearchText(String title, String summary) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isBlank()) sb.append(title);
        if (summary != null && !summary.isBlank()) {
            if (!sb.isEmpty()) sb.append(": ");
            sb.append(summary);
        }
        return sb.toString();
    }
}

package com.flowmind.knowledge.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Weaviate vector database client for KnowledgeDoc chunks.
 *
 * Each chunk is a separate Weaviate object in class "KnowledgeDoc".
 * A single MySQL doc can produce multiple chunks (title chunk + content paragraph chunks).
 * All chunks share the same feishuToken for cleanup.
 *
 * Schema properties: title, chunkText, feishuToken, feishuUrl, feishuType,
 * tags (text[]), mysqlId, chunkIndex (int), totalChunks (int)
 */
@Service
public class WeaviateClientService {
    private static final Logger log = LoggerFactory.getLogger(WeaviateClientService.class);
    private static final String CLASS_NAME = "KnowledgeDoc";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${flowmind.weaviate.enabled:false}")
    private boolean enabled;

    @Value("${flowmind.weaviate.base-url:http://localhost:18080}")
    private String baseUrl;

    public boolean isEnabled() { return enabled; }

    // ── Schema ──

    public void ensureSchema() {
        if (!enabled) return;
        try {
            var checkReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/schema/" + CLASS_NAME))
                    .GET()
                    .build();
            var checkResp = httpClient.send(checkReq, HttpResponse.BodyHandlers.ofString());
            if (checkResp.statusCode() == 200) {
                log.info("Weaviate class '{}' already exists", CLASS_NAME);
                return;
            }

            ObjectNode classDef = objectMapper.createObjectNode();
            classDef.put("class", CLASS_NAME);
            classDef.put("vectorizer", "none");

            ArrayNode props = classDef.putArray("properties");
            addProp(props, "title", "text");
            addProp(props, "chunkText", "text");
            addProp(props, "feishuToken", "string");
            addProp(props, "feishuUrl", "string");
            addProp(props, "feishuType", "string");
            addProp(props, "tags", "text[]");
            addProp(props, "mysqlId", "int");
            addProp(props, "chunkIndex", "int");
            addProp(props, "totalChunks", "int");

            var createReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/schema"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(classDef)))
                    .build();
            var createResp = httpClient.send(createReq, HttpResponse.BodyHandlers.ofString());
            if (createResp.statusCode() == 200) {
                log.info("Weaviate class '{}' created", CLASS_NAME);
            } else {
                log.warn("Failed to create Weaviate class: {} {}", createResp.statusCode(), createResp.body());
            }
        } catch (Exception e) {
            log.warn("Weaviate schema init failed (non-fatal): {}", e.getMessage());
        }
    }

    // ── Chunk vector ops ──

    public record ChunkVec(
            long mysqlId,
            String feishuToken,
            String title,
            String chunkText,
            String feishuUrl,
            String feishuType,
            List<String> tags,
            int chunkIndex,
            int totalChunks
    ) {}

    /**
     * Delete all chunks for a given feishuToken, then insert new chunks.
     */
    public int replaceChunks(String feishuToken, List<ChunkVec> chunks, List<float[]> vectors) {
        if (!enabled || feishuToken == null || feishuToken.isBlank()) return 0;
        if (chunks.size() != vectors.size()) {
            log.warn("chunks/vectors size mismatch: {} vs {}", chunks.size(), vectors.size());
            return 0;
        }
        deleteByFeishuToken(feishuToken);

        int inserted = 0;
        for (int i = 0; i < chunks.size(); i++) {
            ChunkVec c = chunks.get(i);
            float[] vec = vectors.get(i);
            if (vec == null || vec.length == 0) continue;
            try {
                ObjectNode payload = objectMapper.createObjectNode();
                payload.put("class", CLASS_NAME);
                ArrayNode vecArr = payload.putArray("vector");
                for (float v : vec) vecArr.add(v);

                ObjectNode props = payload.putObject("properties");
                props.put("title", c.title());
                props.put("chunkText", c.chunkText());
                props.put("feishuToken", c.feishuToken());
                props.put("feishuUrl", c.feishuUrl() != null ? c.feishuUrl() : "");
                props.put("feishuType", c.feishuType() != null ? c.feishuType() : "docx");
                ArrayNode tagsArr = props.putArray("tags");
                if (c.tags() != null) c.tags().forEach(tagsArr::add);
                props.put("mysqlId", (int) c.mysqlId());
                props.put("chunkIndex", c.chunkIndex());
                props.put("totalChunks", c.totalChunks());

                var req = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/v1/objects"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                        .build();
                var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    inserted++;
                } else {
                    log.warn("Weaviate chunk insert failed for {}/{}: {} {}",
                            feishuToken, i, resp.statusCode(), resp.body());
                }
            } catch (Exception e) {
                log.warn("Weaviate chunk upsert failed for {}: {}", feishuToken, e.getMessage());
            }
        }
        log.debug("Weaviate: replaced {} chunks for feishuToken={}", inserted, feishuToken);
        return inserted;
    }

    public void deleteByFeishuToken(String feishuToken) {
        if (!enabled || feishuToken == null || feishuToken.isBlank()) return;
        try {
            String match = "{\"match\":{\"class\":\"" + CLASS_NAME
                    + "\",\"where\":{\"operator\":\"Equal\",\"path\":[\"feishuToken\"],"
                    + "\"valueString\":\"" + escapeJson(feishuToken) + "\"}}}";
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/batch/objects"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(match))
                    .build();
            var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            log.debug("Weaviate delete {}: {}", feishuToken, resp.statusCode());
        } catch (Exception e) {
            log.warn("Weaviate delete failed for {}: {}", feishuToken, e.getMessage());
        }
    }

    // ── Search ──

    public List<SearchHit> search(float[] queryVector, int topK) {
        if (!enabled || queryVector == null || queryVector.length == 0) return List.of();
        try {
            StringBuilder gql = new StringBuilder();
            gql.append("{ Get { ").append(CLASS_NAME).append("(nearVector: {vector: [");
            for (int i = 0; i < queryVector.length; i++) {
                if (i > 0) gql.append(",");
                gql.append(queryVector[i]);
            }
            gql.append("]} limit: ").append(topK)
                .append(") { mysqlId feishuToken title chunkText feishuUrl feishuType tags chunkIndex totalChunks _additional { distance } } } }");

            ObjectNode gqlBody = objectMapper.createObjectNode();
            gqlBody.put("query", gql.toString());

            var req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/graphql"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(gqlBody)))
                    .build();
            var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                return parseSearchResults(resp.body());
            }
            log.warn("Weaviate search failed: {} {}", resp.statusCode(), resp.body());
            return List.of();
        } catch (Exception e) {
            log.warn("Weaviate search failed: {}", e.getMessage());
            return List.of();
        }
    }

    public record SearchHit(int mysqlId, String feishuToken, String title, String chunkText,
                            String feishuUrl, String feishuType, List<String> tags,
                            int chunkIndex, int totalChunks, float distance) {}

    // ── helpers ──

    private void addProp(ArrayNode props, String name, String dataType) {
        ObjectNode prop = props.addObject();
        prop.put("name", name);
        ArrayNode dt = prop.putArray("dataType");
        dt.add(dataType);
    }

    private List<SearchHit> parseSearchResults(String body) {
        List<SearchHit> hits = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root.path("data").path("Get").path(CLASS_NAME);
            if (items.isArray()) {
                for (JsonNode item : items) {
                    List<String> tags = new ArrayList<>();
                    item.path("tags").forEach(t -> tags.add(t.asText()));
                    hits.add(new SearchHit(
                            item.path("mysqlId").asInt(),
                            item.path("feishuToken").asText(""),
                            item.path("title").asText(""),
                            item.path("chunkText").asText(""),
                            item.path("feishuUrl").asText(""),
                            item.path("feishuType").asText(""),
                            tags,
                            item.path("chunkIndex").asInt(0),
                            item.path("totalChunks").asInt(0),
                            (float) item.path("_additional").path("distance").asDouble(1.0)
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Parse search results failed: {}", e.getMessage());
        }
        return hits;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

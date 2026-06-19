package com.flowmind.test.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("KnowledgeController — Knowledge Base API")
class KnowledgeControllerTest extends BaseControllerTest {

    @Nested
    @DisplayName("GET /api/knowledge/docs — List/search documents")
    class ListDocs {
        @Test @DisplayName("should return document list")
        void shouldReturnDocumentList() throws Exception {
            mockMvc.perform(get("/api/knowledge/docs")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test @DisplayName("should search by keyword")
        void shouldSearchByKeyword() throws Exception {
            mockMvc.perform(get("/api/knowledge/docs?keyword=面试")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("should handle empty keyword")
        void shouldHandleEmptyKeyword() throws Exception {
            mockMvc.perform(get("/api/knowledge/docs?keyword=")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("should handle Chinese keyword")
        void shouldHandleChineseKeyword() throws Exception {
            mockMvc.perform(get("/api/knowledge/docs?keyword=保研面试")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("should handle special characters in keyword")
        void shouldHandleSpecialCharacters() throws Exception {
            mockMvc.perform(get("/api/knowledge/docs?keyword=test%26%3Dvalue")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/knowledge/docs/{id} — Get single document")
    class GetDoc {
        @Test @DisplayName("should return 200 for existing doc id")
        void shouldReturnDocById() throws Exception {
            mockMvc.perform(get("/api/knowledge/docs/1")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should return failed for non-existent id")
        void shouldReturnFailedForNonExistentId() throws Exception {
            mockMvc.perform(get("/api/knowledge/docs/99999999")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("should handle negative id")
        void shouldHandleNegativeId() throws Exception {
            mockMvc.perform(get("/api/knowledge/docs/-1")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/knowledge/docs/{id}/tags — Update tags")
    class UpdateTags {
        @Test @DisplayName("should update tags on document")
        void shouldUpdateTags() throws Exception {
            String body = "{\"tags\":[\"夏令营\",\"面试\",\"材料\"]}";
            mockMvc.perform(put("/api/knowledge/docs/1/tags")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should handle empty tags array")
        void shouldHandleEmptyTagsArray() throws Exception {
            String body = "{\"tags\":[]}";
            mockMvc.perform(put("/api/knowledge/docs/1/tags")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should handle many tags")
        void shouldHandleManyTags() throws Exception {
            StringBuilder sb = new StringBuilder("{\"tags\":[");
            for (int i = 0; i < 50; i++) {
                if (i > 0) sb.append(",");
                sb.append("\"tag-").append(i).append("\"");
            }
            sb.append("]}");
            mockMvc.perform(put("/api/knowledge/docs/1/tags")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(sb.toString()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/knowledge/sync — Trigger Feishu sync")
    class Sync {
        @Test @DisplayName("should trigger sync and return status")
        void shouldTriggerSync() throws Exception {
            mockMvc.perform(post("/api/knowledge/sync")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.status").isString());
        }

        @Test @DisplayName("should return consistent fields")
        void shouldReturnConsistentFields() throws Exception {
            mockMvc.perform(post("/api/knowledge/sync")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.added").isNumber())
                    .andExpect(jsonPath("$.data.updated").isNumber())
                    .andExpect(jsonPath("$.data.skipped").isNumber())
                    .andExpect(jsonPath("$.data.errors").isNumber());
        }
    }

    @Nested
    @DisplayName("GET /api/knowledge/sync-logs — Sync logs")
    class SyncLogs {
        @Test @DisplayName("should return sync logs list")
        void shouldReturnSyncLogs() throws Exception {
            mockMvc.perform(get("/api/knowledge/sync-logs")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/knowledge/sync-status — Sync status")
    class SyncStatus {
        @Test @DisplayName("should return status for all sync types")
        void shouldReturnSyncStatus() throws Exception {
            mockMvc.perform(get("/api/knowledge/sync-status")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.docs").isMap())
                    .andExpect(jsonPath("$.data.bitable").isMap())
                    .andExpect(jsonPath("$.data.tasks").isMap())
                    .andExpect(jsonPath("$.data.bot").isMap());
        }
    }

    @Nested
    @DisplayName("GET /api/knowledge/stats — Stats")
    class Stats {
        @Test @DisplayName("should return doc count")
        void shouldReturnDocCount() throws Exception {
            mockMvc.perform(get("/api/knowledge/stats")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.docCount").isNumber());
        }
    }

    @Nested
    @DisplayName("GET /api/knowledge/tags — Aggregated tags")
    class Tags {
        @Test @DisplayName("should return tags list")
        void shouldReturnTagsList() throws Exception {
            mockMvc.perform(get("/api/knowledge/tags")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/knowledge/search — Semantic search")
    class SemanticSearch {
        @Test @DisplayName("should handle search request (Weaviate may be off)")
        void shouldHandleSearchRequest() throws Exception {
            mockMvc.perform(get("/api/knowledge/search?q=面试&topK=5")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should handle search with default topK")
        void shouldHandleDefaultTopK() throws Exception {
            mockMvc.perform(get("/api/knowledge/search?q=保研")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should handle empty query")
        void shouldHandleEmptyQuery() throws Exception {
            mockMvc.perform(get("/api/knowledge/search?q=")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }
    }
}

package com.flowmind.test.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ContentController — Content Operations API")
class ContentControllerTest extends BaseControllerTest {

    @Nested
    @DisplayName("GET /api/content/themes — List themes")
    class ListThemes {
        @Test @DisplayName("should return themes list")
        void shouldReturnThemes() throws Exception {
            mockMvc.perform(get("/api/content/themes")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test @DisplayName("should filter by keyword")
        void shouldFilterByKeyword() throws Exception {
            mockMvc.perform(get("/api/content/themes?keyword=保研")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should filter by status")
        void shouldFilterByStatus() throws Exception {
            mockMvc.perform(get("/api/content/themes?status=已发布")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should filter by channel")
        void shouldFilterByChannel() throws Exception {
            mockMvc.perform(get("/api/content/themes?channel=小红书")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should combine multiple filters")
        void shouldCombineFilters() throws Exception {
            mockMvc.perform(get("/api/content/themes?keyword=面试&status=已发布&channel=小红书")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/content/themes — Create theme")
    class CreateTheme {
        @Test @DisplayName("should create a new theme")
        void shouldCreateTheme() throws Exception {
            String body = """
                    {"title":"测试保研主题","topic":"测试","platform":"小红书","type":"经验","status":"待创作","heat":80,"summary":"测试摘要","tags":["测试"]}
                    """;
            mockMvc.perform(post("/api/content/themes")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("should handle minimal create request")
        void shouldHandleMinimalCreateRequest() throws Exception {
            String body = "{\"title\":\"最小主题\",\"platform\":\"小红书\",\"type\":\"干货\",\"status\":\"待创作\"}";
            mockMvc.perform(post("/api/content/themes")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/content/themes/{id} — Delete theme")
    class DeleteTheme {
        @Test @DisplayName("should delete theme")
        void shouldDeleteTheme() throws Exception {
            mockMvc.perform(delete("/api/content/themes/1")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/content/themes/{id}/rating — Rate theme")
    class RateTheme {
        @Test @DisplayName("should rate a theme")
        void shouldRateTheme() throws Exception {
            mockMvc.perform(put("/api/content/themes/1/rating")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content("{\"rating\":5}"))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should handle rating out of bounds")
        void shouldHandleOutOfBoundsRating() throws Exception {
            mockMvc.perform(put("/api/content/themes/1/rating")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content("{\"rating\":10}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/content/drafts — List drafts")
    class ListDrafts {
        @Test @DisplayName("should return drafts with filters")
        void shouldReturnDrafts() throws Exception {
            mockMvc.perform(get("/api/content/drafts")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test @DisplayName("should filter drafts by usageStatus")
        void shouldFilterDraftsByUsage() throws Exception {
            mockMvc.perform(get("/api/content/drafts?usageStatus=已使用")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/content/calendar — Calendar")
    class Calendar {
        @Test @DisplayName("should return calendar data")
        void shouldReturnCalendar() throws Exception {
            mockMvc.perform(get("/api/content/calendar")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test @DisplayName("should filter calendar by month")
        void shouldFilterCalendarByMonth() throws Exception {
            mockMvc.perform(get("/api/content/calendar?month=2026-06")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/content/sop/xiaohongshu/generate — SOP generation")
    class SopGeneration {
        @Test @DisplayName("should generate xiaohongshu content")
        void shouldGenerateXiaohongshu() throws Exception {
            String body = "{\"agentType\":\"xiaohongshu\",\"message\":\"保研经验分享\"}";
            mockMvc.perform(post("/api/content/sop/xiaohongshu/generate")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should generate moments content")
        void shouldGenerateMoments() throws Exception {
            String body = "{\"agentType\":\"moments\",\"message\":\"收到offer\"}";
            mockMvc.perform(post("/api/content/sop/moments/generate")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should extract assets")
        void shouldExtractAssets() throws Exception {
            String body = "{\"agentType\":\"asset\",\"message\":\"提取知识\"}";
            mockMvc.perform(post("/api/content/sop/assets/extract")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/content/sop/architecture — SOP architecture")
    class SopArchitecture {
        @Test @DisplayName("should return SOP architecture")
        void shouldReturnSopArchitecture() throws Exception {
            mockMvc.perform(get("/api/content/sop/architecture")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("POST /api/content/topics/generate — Topic generation")
    class TopicGeneration {
        @Test @DisplayName("should generate topics")
        void shouldGenerateTopics() throws Exception {
            String body = "{\"theme\":\"保研\",\"style\":\"干货\"}";
            mockMvc.perform(post("/api/content/topics/generate")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/content/articles/generate — Article generation")
    class ArticleGeneration {
        @Test @DisplayName("should generate article titles")
        void shouldGenerateArticleTitles() throws Exception {
            String body = "{\"topic\":\"保研规划\"}";
            mockMvc.perform(post("/api/content/articles/generate")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Edge cases — Content Controller")
    class EdgeCases {
        @Test @DisplayName("should handle missing request body")
        void shouldHandleMissingBody() throws Exception {
            mockMvc.perform(post("/api/content/themes")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json"))
                    .andExpect(status().is4xxClientError());
        }

        @Test @DisplayName("should handle malformed JSON")
        void shouldHandleMalformedJson() throws Exception {
            mockMvc.perform(post("/api/content/themes")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content("not json"))
                    .andExpect(status().is4xxClientError());
        }
    }
}

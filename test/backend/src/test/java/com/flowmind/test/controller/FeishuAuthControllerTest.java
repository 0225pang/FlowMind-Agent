package com.flowmind.test.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("FeishuController & AuthController — Feishu/Auth API")
class FeishuAuthControllerTest extends BaseControllerTest {

    @Nested
    @DisplayName("POST /api/auth/login — User login")
    class Login {
        @Test @DisplayName("should login with valid credentials")
        void shouldLoginWithValidCredentials() throws Exception {
            String body = "{\"username\":\"admin\",\"password\":\"123456\"}";
            mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("should handle invalid credentials")
        void shouldHandleInvalidCredentials() throws Exception {
            String body = "{\"username\":\"\",\"password\":\"\"}";
            mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should handle missing fields")
        void shouldHandleMissingFields() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/register — User register")
    class Register {
        @Test @DisplayName("should register new user")
        void shouldRegisterNewUser() throws Exception {
            String body = "{\"username\":\"newuser\",\"password\":\"pass123\"}";
            mockMvc.perform(post("/api/auth/register")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/users/me — Current user")
    class CurrentUser {
        @Test @DisplayName("should return current user profile")
        void shouldReturnCurrentUser() throws Exception {
            mockMvc.perform(get("/api/users/me")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("GET /api/feishu/sync/status — Sync status")
    class FeishuSyncStatus {
        @Test @DisplayName("should return sync connection status")
        void shouldReturnSyncStatus() throws Exception {
            mockMvc.perform(get("/api/feishu/sync/status")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("GET /api/feishu/lark-cli/status — Lark CLI status")
    class LarkCliStatus {
        @Test @DisplayName("should return CLI availability")
        void shouldReturnCliAvailability() throws Exception {
            mockMvc.perform(get("/api/feishu/lark-cli/status")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("GET /api/feishu/knowledge-base/files — KB files")
    class KnowledgeBaseFiles {
        @Test @DisplayName("should return knowledge base file list")
        void shouldReturnKbFiles() throws Exception {
            mockMvc.perform(get("/api/feishu/knowledge-base/files")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/feishu/docs/create — Create Feishu doc")
    class CreateFeishuDoc {
        @Test @DisplayName("should attempt to create doc (may fail in test)")
        void shouldAttemptCreateDoc() throws Exception {
            String body = "{\"title\":\"测试文档\",\"content\":\"测试内容\"}";
            mockMvc.perform(post("/api/feishu/docs/create")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/feishu/docs/fetch — Fetch Feishu doc")
    class FetchFeishuDoc {
        @Test @DisplayName("should attempt to fetch doc")
        void shouldAttemptFetchDoc() throws Exception {
            String body = "{\"docToken\":\"test123\"}";
            mockMvc.perform(post("/api/feishu/docs/fetch")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/feishu/sync/docs — Trigger sync")
    class TriggerSync {
        @Test @DisplayName("should trigger docs sync")
        void shouldTriggerDocsSync() throws Exception {
            mockMvc.perform(post("/api/feishu/sync/docs")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("should trigger bitable sync")
        void shouldTriggerBitableSync() throws Exception {
            mockMvc.perform(post("/api/feishu/sync/bitable")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should trigger tasks sync")
        void shouldTriggerTasksSync() throws Exception {
            mockMvc.perform(post("/api/feishu/sync/tasks")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/feishu/bot/push — Push bot message")
    class BotPush {
        @Test @DisplayName("should push bot message")
        void shouldPushBotMessage() throws Exception {
            String body = "{\"message\":\"测试推送\"}";
            mockMvc.perform(post("/api/feishu/bot/push")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/feishu/logs — Sync logs")
    class FeishuLogs {
        @Test @DisplayName("should return feishu sync logs")
        void shouldReturnFeishuLogs() throws Exception {
            mockMvc.perform(get("/api/feishu/logs")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/analytics/overview — Analytics")
    class Analytics {
        @Test @DisplayName("should return analytics overview")
        void shouldReturnAnalyticsOverview() throws Exception {
            mockMvc.perform(get("/api/analytics/overview")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("should return student distribution")
        void shouldReturnStudentDistribution() throws Exception {
            mockMvc.perform(get("/api/analytics/student-distribution")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should return content stats")
        void shouldReturnContentStats() throws Exception {
            mockMvc.perform(get("/api/analytics/content-stats")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should return application funnel")
        void shouldReturnApplicationFunnel() throws Exception {
            mockMvc.perform(get("/api/analytics/application-funnel")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should return school deadlines")
        void shouldReturnSchoolDeadlines() throws Exception {
            mockMvc.perform(get("/api/analytics/school-deadlines")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }
    }
}

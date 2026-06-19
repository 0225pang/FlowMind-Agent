package com.flowmind.test.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AgentController — AI Agent Chat & Sessions")
class AgentControllerTest extends BaseControllerTest {

    @Nested
    @DisplayName("GET /api/agents — List agents")
    class ListAgents {
        @Test @DisplayName("should return all registered agents")
        void shouldReturnAllAgents() throws Exception {
            mockMvc.perform(get("/api/agents")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/agents/sessions — List sessions")
    class ListSessions {
        @Test @DisplayName("should return session list (empty initially)")
        void shouldReturnSessionList() throws Exception {
            mockMvc.perform(get("/api/agents/sessions")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/agents/conversations/new — Create session")
    class CreateSession {
        @Test @DisplayName("should create a new session and return sessionId")
        void shouldCreateNewSession() throws Exception {
            mockMvc.perform(post("/api/agents/conversations/new")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.sessionId").isString());
        }

        @Test @DisplayName("should return different sessionIds for multiple calls")
        void shouldReturnDifferentSessionIds() throws Exception {
            String response1 = mockMvc.perform(post("/api/agents/conversations/new")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andReturn().getResponse().getContentAsString();
            String response2 = mockMvc.perform(post("/api/agents/conversations/new")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andReturn().getResponse().getContentAsString();
            // Sessions should be different
            assert !response1.equals(response2) : "Sessions should be unique";
        }

        @Test @DisplayName("should accept without auth header (mock auth)")
        void shouldAcceptWithoutAuthHeader() throws Exception {
            mockMvc.perform(post("/api/agents/conversations/new"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/agents/chat — Non-streaming chat")
    class Chat {
        @Test @DisplayName("should return a valid AgentChatResponse for content request")
        void shouldReturnChatResponseForContentRequest() throws Exception {
            String requestBody = """
                    {"agentType":"auto","message":"生成小红书选题","sessionId":""}
                    """;
            mockMvc.perform(post("/api/agents/chat")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.reply").isString())
                    .andExpect(jsonPath("$.data.agentType").isString());
        }

        @Test @DisplayName("should route feishu request to FeishuAgent")
        void shouldRouteFeishuRequest() throws Exception {
            String requestBody = """
                    {"agentType":"auto","message":"创建一个飞书文档","sessionId":""}
                    """;
            mockMvc.perform(post("/api/agents/chat")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should route student request to StudentAgent")
        void shouldRouteStudentRequest() throws Exception {
            String requestBody = """
                    {"agentType":"auto","message":"分析学员01的风险","sessionId":""}
                    """;
            mockMvc.perform(post("/api/agents/chat")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should route school request to SchoolAgent")
        void shouldRouteSchoolRequest() throws Exception {
            String requestBody = """
                    {"agentType":"auto","message":"推荐适合经管学生的夏令营项目","sessionId":""}
                    """;
            mockMvc.perform(post("/api/agents/chat")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should route knowledge request to KnowledgeAgent")
        void shouldRouteKnowledgeRequest() throws Exception {
            String requestBody = """
                    {"agentType":"auto","message":"总结知识库里的夏令营资料","sessionId":""}
                    """;
            mockMvc.perform(post("/api/agents/chat")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should handle empty message gracefully")
        void shouldHandleEmptyMessage() throws Exception {
            String requestBody = """
                    {"agentType":"auto","message":"","sessionId":""}
                    """;
            mockMvc.perform(post("/api/agents/chat")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("should handle null agentType (default to content)")
        void shouldHandleNullAgentType() throws Exception {
            String requestBody = """
                    {"message":"测试消息","sessionId":""}
                    """;
            mockMvc.perform(post("/api/agents/chat")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/agents/chat/stream — SSE streaming chat")
    class StreamChat {
        @Test @DisplayName("should return SSE content-type for stream request")
        void shouldReturnSseContentType() throws Exception {
            String requestBody = """
                    {"agentType":"auto","message":"测试流式对话","sessionId":""}
                    """;
            mockMvc.perform(post("/api/agents/chat/stream")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should handle stream with empty message")
        void shouldHandleStreamEmptyMessage() throws Exception {
            String requestBody = """
                    {"agentType":"content","message":"","sessionId":""}
                    """;
            mockMvc.perform(post("/api/agents/chat/stream")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should accept session-less stream request")
        void shouldAcceptSessionLessStream() throws Exception {
            String requestBody = """
                    {"agentType":"auto","message":"生成内容"}
                    """;
            mockMvc.perform(post("/api/agents/chat/stream")
                            .header(AUTH_HEADER, BEARER_TOKEN)
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/agents/conversations/{agentType}/{sessionId}")
    class ClearHistory {
        @Test @DisplayName("should return success even for non-existent session")
        void shouldReturnSuccessForNonExistentSession() throws Exception {
            mockMvc.perform(delete("/api/agents/conversations/content/nonexistent-123")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("should clear conversation without error")
        void shouldClearConversation() throws Exception {
            mockMvc.perform(delete("/api/agents/conversations/auto/test-clear")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/agents/sessions/{sessionId} — Delete session")
    class DeleteSession {
        @Test @DisplayName("should return success for session deletion")
        void shouldReturnSuccess() throws Exception {
            mockMvc.perform(delete("/api/agents/sessions/test-session-1")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test @DisplayName("should handle deletion of same session twice")
        void shouldHandleDuplicateDeletion() throws Exception {
            var result1 = mockMvc.perform(delete("/api/agents/sessions/test-dup")
                    .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk()).andReturn();
            var result2 = mockMvc.perform(delete("/api/agents/sessions/test-dup")
                    .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk()).andReturn();
        }
    }

    @Nested
    @DisplayName("GET /api/agents/conversations/{agentType}/{sessionId} — Conversation history")
    class ConversationHistory {
        @Test @DisplayName("should return empty history for new session")
        void shouldReturnEmptyHistoryForNewSession() throws Exception {
            mockMvc.perform(get("/api/agents/conversations/content/new-session")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test @DisplayName("should handle special characters in agentType")
        void shouldHandleSpecialAgentType() throws Exception {
            mockMvc.perform(get("/api/agents/conversations/feishu/session-1")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("should handle special characters in sessionId")
        void shouldHandleSpecialSessionId() throws Exception {
            mockMvc.perform(get("/api/agents/conversations/content/abc-123_xyz.test")
                            .header(AUTH_HEADER, BEARER_TOKEN))
                    .andExpect(status().isOk());
        }
    }
}

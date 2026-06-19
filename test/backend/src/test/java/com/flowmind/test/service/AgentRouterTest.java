package com.flowmind.test.service;

import com.flowmind.agent.dto.AgentRequest;
import com.flowmind.agent.dto.AgentResponse;
import com.flowmind.agent.extension.AgentExtension;
import com.flowmind.agent.core.Agent;
import com.flowmind.agent.llm.MockLLMClient;
import com.flowmind.agent.service.AgentRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AgentRouter — Route Logic Unit Tests")
class AgentRouterTest {

    private AgentRouter router;

    @BeforeEach
    void setUp() {
        // Create router with stub agents
        Agent contentAgent = stubAgent("ContentAgent", "content agent");
        Agent knowledgeAgent = stubAgent("KnowledgeAgent", "knowledge agent");
        Agent feishuAgent = stubAgent("FeishuAgent", "feishu agent");
        Agent studentAgent = stubAgent("StudentAgent", "student agent");
        Agent schoolAgent = stubAgent("SchoolAgent", "school agent");

        router = new AgentRouter(List.of(
                contentAgent, knowledgeAgent, feishuAgent, studentAgent, schoolAgent
        ));
    }

    private Agent stubAgent(String name, String description) {
        return new Agent() {
            @Override public String getName() { return name; }
            @Override public String getDescription() { return description; }
            @Override
            public AgentResponse execute(AgentRequest request) {
                return AgentResponse.of(name, "response from " + name, List.of());
            }
        };
    }

    @Nested
    @DisplayName("route — Agent selection")
    class Route {
        @Test @DisplayName("should route feishu requests to FeishuAgent")
        void shouldRouteFeishuRequests() {
            AgentRequest req = new AgentRequest();
            req.setAgentType("auto");
            req.setMessage("创建一个飞书文档");
            AgentResponse resp = router.route(req);
            assertTrue(resp.getAgentType().contains("feishu"),
                    "Expected feishu but got " + resp.getAgentType());
        }

        @Test @DisplayName("should route student requests to StudentAgent")
        void shouldRouteStudentRequests() {
            AgentRequest req = new AgentRequest();
            req.setAgentType("auto");
            req.setMessage("分析学员风险");
            AgentResponse resp = router.route(req);
            assertTrue(resp.getAgentType().contains("student"),
                    "Expected student but got " + resp.getAgentType());
        }

        @Test @DisplayName("should route school requests to SchoolAgent")
        void shouldRouteSchoolRequests() {
            AgentRequest req = new AgentRequest();
            req.setAgentType("auto");
            req.setMessage("推荐夏令营项目");
            AgentResponse resp = router.route(req);
            assertTrue(resp.getAgentType().contains("school"),
                    "Expected school but got " + resp.getAgentType());
        }

        @Test @DisplayName("should route content requests to ContentAgent")
        void shouldRouteContentRequests() {
            AgentRequest req = new AgentRequest();
            req.setAgentType("auto");
            req.setMessage("生成小红书选题");
            AgentResponse resp = router.route(req);
            assertTrue(resp.getAgentType().contains("content"),
                    "Expected content but got " + resp.getAgentType());
        }

        @Test @DisplayName("should default to content for unrecognized messages")
        void shouldDefaultToContent() {
            AgentRequest req = new AgentRequest();
            req.setAgentType("auto");
            req.setMessage("随便聊聊");
            AgentResponse resp = router.route(req);
            assertNotNull(resp);
            assertEquals("content", resp.getAgentType());
        }

        @Test @DisplayName("should use explicit agent type when provided")
        void shouldUseExplicitAgentType() {
            AgentRequest req = new AgentRequest();
            req.setAgentType("feishu");
            req.setMessage("任何内容");
            AgentResponse resp = router.route(req);
            assertEquals("feishu", resp.getAgentType());
        }

        @Test @DisplayName("should handle null message")
        void shouldHandleNullMessage() {
            AgentRequest req = new AgentRequest();
            req.setAgentType("auto");
            req.setMessage(null);
            AgentResponse resp = router.route(req);
            assertNotNull(resp);
            assertNotNull(resp.getReply());
        }

        @Test @DisplayName("should handle empty message")
        void shouldHandleEmptyMessage() {
            AgentRequest req = new AgentRequest();
            req.setAgentType("auto");
            req.setMessage("");
            AgentResponse resp = router.route(req);
            assertNotNull(resp);
        }
    }

    @Nested
    @DisplayName("stream — Streaming output")
    class Stream {
        @Test @DisplayName("should call onDelta with content")
        void shouldCallOnDelta() {
            AgentRequest req = new AgentRequest();
            req.setAgentType("content");
            req.setMessage("测试内容生成");

            AtomicReference<String> received = new AtomicReference<>("");
            router.stream(req, delta -> received.set(received.get() + delta));

            assertFalse(received.get().isEmpty(), "Should receive streamed content");
        }

        @Test @DisplayName("should handle stream for all agent types")
        void shouldHandleStreamForAllTypes() {
            String[] agentTypes = {"content", "knowledge", "feishu", "student", "school"};
            for (String type : agentTypes) {
                AgentRequest req = new AgentRequest();
                req.setAgentType(type);
                req.setMessage("测试消息");
                AtomicReference<String> received = new AtomicReference<>("");
                router.stream(req, delta -> received.set(received.get() + delta));
                assertFalse(received.get().isEmpty(),
                        "Agent " + type + " should produce output");
            }
        }
    }

    @Nested
    @DisplayName("listAgents — Agent catalog")
    class ListAgents {
        @Test @DisplayName("should return all registered agents")
        void shouldListAllAgents() {
            List<Map<String, String>> agents = router.listAgents();
            assertFalse(agents.isEmpty());
            // First entry is FlowMindAgent (auto)
            Map<String, String> auto = agents.get(0);
            assertEquals("auto", auto.get("type"));
        }

        @Test @DisplayName("should have unique agent types")
        void shouldHaveUniqueAgentTypes() {
            List<Map<String, String>> agents = router.listAgents();
            long uniqueTypes = agents.stream()
                    .map(a -> a.get("type"))
                    .distinct()
                    .count();
            assertEquals(agents.size(), uniqueTypes, "Agent types should be unique");
        }
    }
}

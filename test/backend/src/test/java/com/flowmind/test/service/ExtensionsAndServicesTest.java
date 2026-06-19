package com.flowmind.test.service;

import com.flowmind.agent.extension.AgentExtension;
import com.flowmind.agent.extension.DefaultAgentExtensions;
import com.flowmind.agent.extension.LarkCliMcpExtension;
import com.flowmind.agent.extension.McpToolProvider;
import com.flowmind.agent.llm.MockLLMClient;
import com.flowmind.agent.service.ConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Extensions & Core Services — Unit Tests")
class ExtensionsAndServicesTest {

    @Nested
    @DisplayName("LarkCliMcpExtension — MCP tool registration")
    class LarkCliMcpExtensionTest {
        private LarkCliMcpExtension extension;

        @BeforeEach
        void setUp() {
            extension = new LarkCliMcpExtension();
        }

        @Test @DisplayName("should report type as mcp")
        void shouldReportMcpType() {
            assertEquals("mcp", extension.type());
        }

        @Test @DisplayName("should support feishu agent type")
        void shouldSupportFeishu() {
            assertTrue(extension.supports("feishu"));
        }

        @Test @DisplayName("should support knowledge agent type")
        void shouldSupportKnowledge() {
            assertTrue(extension.supports("knowledge"));
        }

        @Test @DisplayName("should support content agent type")
        void shouldSupportContent() {
            assertTrue(extension.supports("content"));
        }

        @Test @DisplayName("should NOT support student agent type")
        void shouldNotSupportStudent() {
            assertFalse(extension.supports("student"));
        }

        @Test @DisplayName("should NOT support school agent type")
        void shouldNotSupportSchool() {
            assertFalse(extension.supports("school"));
        }

        @Test @DisplayName("should have non-empty description")
        void shouldHaveNonEmptyDescription() {
            assertNotNull(extension.description());
            assertFalse(extension.description().isBlank());
        }

        @Test @DisplayName("should have non-empty name")
        void shouldHaveNonEmptyName() {
            assertNotNull(extension.name());
            assertFalse(extension.name().isBlank());
        }
    }

    @Nested
    @DisplayName("DefaultAgentExtensions — Extension collection")
    class DefaultAgentExtensionsTest {
        @Test @DisplayName("should create empty extensions list")
        void shouldCreateEmptyExtensions() {
            DefaultAgentExtensions d = new DefaultAgentExtensions(List.of());
            assertNotNull(d.allExtensions());
            assertTrue(d.allExtensions().isEmpty());
        }

        @Test @DisplayName("should hold provided extensions")
        void shouldHoldProvidedExtensions() {
            LarkCliMcpExtension ext = new LarkCliMcpExtension();
            DefaultAgentExtensions d = new DefaultAgentExtensions(List.of(ext));
            assertEquals(1, d.allExtensions().size());
        }
    }

    @Nested
    @DisplayName("McpToolProvider — Interface contract")
    class McpToolProviderTest {
        @Test @DisplayName("should report mcp type")
        void shouldReportMcpType() {
            McpToolProvider provider = new McpToolProvider() {
                @Override public String name() { return "test-tool"; }
                @Override public String description() { return "test tool"; }
                @Override public boolean supports(String agentType) { return true; }
            };
            assertEquals("mcp", provider.type());
        }
    }

    @Nested
    @DisplayName("AgentExtension — Interface contract")
    class AgentExtensionTest {
        @Test @DisplayName("should have default type as extension")
        void shouldHaveDefaultType() {
            AgentExtension ext = new AgentExtension() {
                @Override public String name() { return "test"; }
                @Override public String description() { return "test desc"; }
                @Override public boolean supports(String agentType) { return true; }
            };
            assertEquals("extension", ext.type());
        }
    }
}

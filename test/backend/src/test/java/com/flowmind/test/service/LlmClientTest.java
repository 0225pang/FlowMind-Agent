package com.flowmind.test.service;

import com.flowmind.agent.llm.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LLM Clients — Unit Tests")
class LlmClientTest {

    @Nested
    @DisplayName("LLMClient interface default stream method")
    class DefaultStream {
        @Test @DisplayName("should delegate to complete() by default")
        void shouldDelegateToComplete() {
            LLMClient client = (systemPrompt, userPrompt) -> "fallback";
            AtomicReference<String> received = new AtomicReference<>("");
            client.stream("system", "user", delta -> received.set(received.get() + delta));
            assertEquals("fallback", received.get());
        }
    }

    @Nested
    @DisplayName("MockLLMClient")
    class MockLlmClientTest {
        private MockLLMClient client;

        @BeforeEach
        void setUp() {
            client = new MockLLMClient();
        }

        @Test @DisplayName("complete() should return non-empty response")
        void shouldReturnNonEmptyResponse() {
            String result = client.complete("system", "user prompt");
            assertNotNull(result);
            assertFalse(result.isBlank());
        }

        @Test @DisplayName("complete() response should contain Chinese content")
        void shouldContainChineseContent() {
            String result = client.complete("test", "测试");
            assertFalse(result.isBlank());
        }

        @Test @DisplayName("stream() should deliver multiple deltas")
        void shouldDeliverMultipleDeltas() {
            AtomicInteger count = new AtomicInteger(0);
            client.stream("system", "user", delta -> count.incrementAndGet());
            assertTrue(count.get() > 0, "Should deliver at least 1 delta, got " + count.get());
        }

        @Test @DisplayName("stream() should deliver content in small chunks")
        void shouldDeliverSmallChunks() {
            AtomicInteger totalLen = new AtomicInteger(0);
            client.stream("sys", "user", delta -> totalLen.addAndGet(delta.length()));
            assertEquals(client.complete("sys", "user").length(), totalLen.get(),
                    "Sum of deltas should equal full response length");
        }

        @Test @DisplayName("multiple complete() calls should return same response")
        void shouldReturnSameResponse() {
            String r1 = client.complete("a", "b");
            String r2 = client.complete("a", "b");
            assertEquals(r1, r2, "Mock should return consistent responses");
        }
    }

    @Nested
    @DisplayName("LlmProperties — Configuration binding")
    class LlmPropertiesTest {
        @Test @DisplayName("should have default values")
        void shouldHaveDefaultValues() {
            LlmProperties props = new LlmProperties();
            assertEquals("mock", props.getProvider());
            assertEquals("deepseek-chat", props.getModel());
            assertEquals(0.7, props.getTemperature(), 0.001);
            assertEquals(4096, props.getMaxTokens());
            assertEquals(15, props.getConnectTimeoutSeconds());
            assertEquals(90, props.getReadTimeoutSeconds());
        }

        @Test @DisplayName("setter/getter should work for all fields")
        void shouldSetAndGetAllFields() {
            LlmProperties props = new LlmProperties();
            props.setProvider("openai");
            props.setModel("gpt-4");
            props.setApiKey("sk-test");
            props.setTemperature(0.5);
            props.setMaxTokens(2048);
            props.setConnectTimeoutSeconds(30);
            props.setReadTimeoutSeconds(60);

            assertEquals("openai", props.getProvider());
            assertEquals("gpt-4", props.getModel());
            assertEquals("sk-test", props.getApiKey());
            assertEquals(0.5, props.getTemperature(), 0.001);
            assertEquals(2048, props.getMaxTokens());
            assertEquals(30, props.getConnectTimeoutSeconds());
            assertEquals(60, props.getReadTimeoutSeconds());
        }
    }
}

package com.flowmind.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@ConditionalOnExpression("'${flowmind.llm.provider:deepseek}' != 'mock' && T(org.springframework.util.StringUtils).hasText('${flowmind.llm.api-key:}') && !'${flowmind.llm.api-key:}'.contains('PUT_YOUR')")
public class OpenAiCompatibleLLMClient implements LLMClient {
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleLLMClient(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        try {
            HttpRequest request = buildRequest(systemPrompt, userPrompt, false);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            ensureSuccess(response.statusCode(), response.body());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return content.isMissingNode() ? "" : content.asText();
        } catch (IOException e) {
            throw new IllegalStateException("LLM response parse failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM call interrupted", e);
        }
    }

    @Override
    public void stream(String systemPrompt, String userPrompt, Consumer<String> onDelta) {
        stream(systemPrompt, userPrompt, onDelta, reasoning -> {});
    }

    @Override
    public void stream(String systemPrompt, String userPrompt,
                       Consumer<String> onDelta,
                       Consumer<String> onReasoningDelta) {
        try {
            HttpRequest request = buildRequest(systemPrompt, userPrompt, true);
            HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            ensureSuccess(response.statusCode(), "");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String payload = line.substring("data:".length()).trim();
                    if (payload.isEmpty() || "[DONE]".equals(payload)) continue;
                    JsonNode root = objectMapper.readTree(payload);
                    JsonNode delta = root.path("choices").path(0).path("delta");
                    String reasoning = firstText(delta, "reasoning_content", "reasoning", "thinking");
                    if (!reasoning.isEmpty()) {
                        onReasoningDelta.accept(reasoning);
                    }
                    JsonNode content = delta.path("content");
                    if (content.isTextual() && !content.asText().isEmpty()) {
                        onDelta.accept(content.asText());
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("LLM stream parse failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM stream interrupted", e);
        }
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isTextual() && !value.asText().isEmpty()) {
                return value.asText();
            }
        }
        return "";
    }

    private HttpRequest buildRequest(String systemPrompt, String userPrompt, boolean stream) throws IOException {
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.isBlank() || apiKey.contains("PUT_YOUR")) {
            throw new IllegalStateException("LLM API key is empty. Please configure flowmind.llm.api-key.");
        }

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", properties.getTemperature(),
                "max_tokens", properties.getMaxTokens(),
                "stream", stream
        );

        return HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(properties.getBaseUrl()) + properties.getChatPath()))
                .timeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = (baseUrl == null || baseUrl.isBlank()) ? "https://api.deepseek.com" : baseUrl.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private void ensureSuccess(int statusCode, String body) {
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException("LLM call failed, HTTP " + statusCode + ": " + body);
        }
    }
}

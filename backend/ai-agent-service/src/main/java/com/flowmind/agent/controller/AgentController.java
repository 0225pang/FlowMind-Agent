package com.flowmind.agent.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmind.agent.dto.AgentRequest;
import com.flowmind.agent.dto.AgentResponse;
import com.flowmind.agent.entity.AgentSessionEntity;
import com.flowmind.agent.entity.ConversationEntity;
import com.flowmind.agent.service.AgentRouter;
import com.flowmind.agent.service.AgentTraceService;
import com.flowmind.agent.service.ConversationService;
import com.flowmind.common.core.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/agents")
public class AgentController {
    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentRouter router;
    private final ConversationService conversationService;
    private final AgentTraceService traceService;
    private final ObjectMapper objectMapper;

    public AgentController(AgentRouter router,
                           ConversationService conversationService,
                           AgentTraceService traceService,
                           ObjectMapper objectMapper) {
        this.router = router;
        this.conversationService = conversationService;
        this.traceService = traceService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ApiResponse<?> agents() {
        return ApiResponse.success(router.listAgents());
    }

    @GetMapping("/sessions")
    public ApiResponse<?> listSessions() {
        List<AgentSessionEntity> sessions = conversationService.listSessions();
        List<Map<String, Object>> result = sessions.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getSessionId());
            m.put("title", s.getTitle() != null ? s.getTitle() : "新会话");
            m.put("agentType", s.getAgentType());
            m.put("turnCount", s.getTurnCount());
            m.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
            m.put("updatedAt", s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null);
            return m;
        }).toList();
        return ApiResponse.success(result);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<?> deleteSession(@PathVariable("sessionId") String sessionId) {
        conversationService.deleteSession(sessionId);
        return ApiResponse.success(Map.of("ok", true));
    }

    @GetMapping("/conversations/{agentType}/{sessionId}")
    public ApiResponse<?> loadHistory(@PathVariable String agentType, @PathVariable String sessionId) {
        List<ConversationEntity> history = conversationService.loadHistory(agentType, sessionId);
        return ApiResponse.success(history);
    }

    @DeleteMapping("/conversations/{agentType}/{sessionId}")
    public ApiResponse<?> clearHistory(@PathVariable String agentType, @PathVariable String sessionId) {
        conversationService.clearSession(agentType, sessionId);
        return ApiResponse.success(Map.of("ok", true));
    }

    @PostMapping("/conversations/new")
    public ApiResponse<?> newSession() {
        return ApiResponse.success(Map.of("sessionId", conversationService.newSessionId()));
    }

    @PostMapping("/chat")
    public ApiResponse<?> chat(@RequestBody AgentRequest request) {
        String sessionId = getOrCreateSession(request);
        saveUserMessage(request, sessionId);

        AgentRequest enriched = enrichWithHistory(request, sessionId);
        String resolvedAgentType = router.resolveAgentType(enriched);
        List<String> processHistory = new ArrayList<>();
        processHistory.add("已选择 " + resolvedAgentType + "，正在查询向量知识库和可用工具。");

        AgentTraceService.TraceBundle trace = traceService.collect(enriched, resolvedAgentType);
        processHistory.add(trace.thinking());

        AgentResponse response = router.route(enriched);
        response.setCards(appendTraceCards(response.getCards(), trace));

        processHistory.add("回答完成，可展开查看本次可见处理过程。");
        saveAssistantMessage(request, sessionId, response.getReply(),
                buildMetadata(resolvedAgentType, trace, processHistory, null));
        response.setSessionId(sessionId);
        return ApiResponse.success(response);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody AgentRequest request) {
        String sessionId = getOrCreateSession(request);
        saveUserMessage(request, sessionId);

        AgentRequest enriched = enrichWithHistory(request, sessionId);
        SseEmitter emitter = new SseEmitter(300_000L);
        StringBuilder fullReply = new StringBuilder();
        StringBuilder modelThinking = new StringBuilder();
        List<String> processHistory = new ArrayList<>();

        CompletableFuture.runAsync(() -> {
            try {
                send(emitter, "session", Map.of("sessionId", sessionId));

                String resolvedAgentType = router.resolveAgentType(enriched);
                String startThinking = "已选择 " + resolvedAgentType + "，正在查询向量知识库和可用工具。";
                processHistory.add(startThinking);
                send(emitter, "thinking", Map.of("content", startThinking));

                AgentTraceService.TraceBundle trace = traceService.collect(enriched, resolvedAgentType);
                send(emitter, "trace", Map.of("items", trace.items(), "agentType", resolvedAgentType));
                processHistory.add(trace.thinking());
                send(emitter, "thinking", Map.of("content", trace.thinking()));

                router.stream(enriched, delta -> {
                    fullReply.append(delta);
                    send(emitter, "delta", Map.of("content", delta));
                }, reasoningDelta -> {
                    modelThinking.append(reasoningDelta);
                    send(emitter, "reasoning", Map.of("content", reasoningDelta));
                });

                String doneThinking = "回答完成，可展开查看本次可见处理过程。";
                processHistory.add(doneThinking);
                saveAssistantMessage(request, sessionId, fullReply.toString(),
                        buildMetadata(resolvedAgentType, trace, processHistory, modelThinking.toString()));
                send(emitter, "thinking", Map.of("content", doneThinking));
                send(emitter, "done", Map.of("ok", true, "sessionId", sessionId));
                emitter.complete();
            } catch (Exception e) {
                log.error("Stream error", e);
                send(emitter, "error", Map.of("message",
                        e.getMessage() == null ? "LLM stream failed" : e.getMessage()));
                emitter.complete();
            }
        });
        return emitter;
    }

    private AgentRequest enrichWithHistory(AgentRequest request, String sessionId) {
        if (!hasText(request.getSessionId())) return request;
        String agentType = request.getAgentTypeOrDefault();
        List<ConversationEntity> history = conversationService.loadRecentContext(agentType, sessionId, 100);
        if (!history.isEmpty()) {
            StringBuilder ctx = new StringBuilder();
            ctx.append("以下是之前的对话历史，共 ").append(history.size() / 2).append(" 轮，请结合上下文回答：\n\n");
            for (ConversationEntity e : history) {
                String label = "user".equals(e.getRole()) ? "用户" : "AI";
                ctx.append("[").append(label).append("] ").append(e.getContent()).append("\n\n");
            }
            Map<String, Object> c = request.getContext();
            if (c == null) c = new HashMap<>();
            c.put("conversationHistory", ctx.toString());
            request.setContext(c);
        }
        return request;
    }

    private void saveUserMessage(AgentRequest request, String sessionId) {
        if (!hasText(request.getSessionId())) return;
        int turn = conversationService.nextTurnIndex(request.getAgentTypeOrDefault(), sessionId);
        conversationService.saveMessage(request.getAgentTypeOrDefault(), sessionId, turn, "user", request.getMessage());
    }

    private void saveAssistantMessage(AgentRequest request, String sessionId, String reply, String metadata) {
        if (!hasText(request.getSessionId())) return;
        int turn = conversationService.nextTurnIndex(request.getAgentTypeOrDefault(), sessionId);
        conversationService.saveMessage(request.getAgentTypeOrDefault(), sessionId, turn, "assistant", reply, metadata);
    }

    private String getOrCreateSession(AgentRequest request) {
        if (hasText(request.getSessionId())) return request.getSessionId();
        String newId = conversationService.newSessionId();
        request.setSessionId(newId);
        return newId;
    }

    private void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String buildMetadata(String resolvedAgentType,
                                 AgentTraceService.TraceBundle trace,
                                 List<String> processHistory,
                                 String modelThinking) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("version", 1);
        metadata.put("agentType", resolvedAgentType);
        metadata.put("traceItems", trace == null ? List.of() : trace.items());
        metadata.put("thinking", processHistory == null || processHistory.isEmpty()
                ? null
                : processHistory.get(processHistory.size() - 1));
        metadata.put("thinkingHistory", processHistory == null ? List.of() : processHistory);
        metadata.put("modelThinking", hasText(modelThinking) ? modelThinking : null);
        metadata.put("createdAt", LocalDateTime.now().toString());
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize agent message metadata", e);
            return null;
        }
    }

    private List<Map<String, Object>> appendTraceCards(List<Map<String, Object>> cards, AgentTraceService.TraceBundle trace) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (cards != null) result.addAll(cards);
        result.add(Map.of("type", "trace", "title", "工具调用", "items", trace.items()));
        result.add(Map.of("type", "thinking", "title", "处理状态", "content", trace.thinking()));
        return result;
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}

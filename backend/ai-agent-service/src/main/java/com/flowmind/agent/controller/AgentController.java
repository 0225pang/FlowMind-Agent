package com.flowmind.agent.controller;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
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

    public AgentController(AgentRouter router, ConversationService conversationService, AgentTraceService traceService) {
        this.router = router;
        this.conversationService = conversationService;
        this.traceService = traceService;
    }

    @GetMapping
    public ApiResponse<?> agents() {
        return ApiResponse.success(router.listAgents());
    }

    // ---- Sessions (like Doubao left panel) ----

    @GetMapping("/sessions")
    public ApiResponse<?> listSessions() {
        List<AgentSessionEntity> sessions = conversationService.listSessions();
        List<Map<String, Object>> result = sessions.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getSessionId());
            m.put("title", s.getTitle() != null ? s.getTitle() : "无标题");
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

    // ---- Conversation history ----

    @GetMapping("/conversations/{agentType}/{sessionId}")
    public ApiResponse<?> loadHistory(@PathVariable String agentType,
                                       @PathVariable String sessionId) {
        List<ConversationEntity> history = conversationService.loadHistory(agentType, sessionId);
        return ApiResponse.success(history);
    }

    @DeleteMapping("/conversations/{agentType}/{sessionId}")
    public ApiResponse<?> clearHistory(@PathVariable String agentType,
                                        @PathVariable String sessionId) {
        conversationService.clearSession(agentType, sessionId);
        return ApiResponse.success(Map.of("ok", true));
    }

    @PostMapping("/conversations/new")
    public ApiResponse<?> newSession() {
        return ApiResponse.success(Map.of("sessionId", conversationService.newSessionId()));
    }

    // ---- Chat ----

    @PostMapping("/chat")
    public ApiResponse<?> chat(@RequestBody AgentRequest request) {
        String sessionId = getOrCreateSession(request);
        if (hasText(request.getSessionId())) {
            int turn = conversationService.nextTurnIndex(request.getAgentTypeOrDefault(), sessionId);
            conversationService.saveMessage(request.getAgentTypeOrDefault(), sessionId, turn,
                    "user", request.getMessage());
        }
        AgentRequest enriched = enrichWithHistory(request, sessionId);
        String resolvedAgentType = router.resolveAgentType(enriched);
        AgentTraceService.TraceBundle trace = traceService.collect(enriched, resolvedAgentType);
        AgentResponse response = router.route(enriched);
        response.setCards(appendTraceCards(response.getCards(), trace));
        if (hasText(request.getSessionId())) {
            int turn = conversationService.nextTurnIndex(request.getAgentTypeOrDefault(), sessionId);
            conversationService.saveMessage(request.getAgentTypeOrDefault(), sessionId, turn,
                    "assistant", response.getReply());
        }
        response.setSessionId(sessionId);
        return ApiResponse.success(response);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody AgentRequest request) {
        String sessionId = getOrCreateSession(request);
        // Save user message
        if (hasText(request.getSessionId())) {
            int turn = conversationService.nextTurnIndex(request.getAgentTypeOrDefault(), sessionId);
            conversationService.saveMessage(request.getAgentTypeOrDefault(), sessionId, turn,
                    "user", request.getMessage());
        }
        AgentRequest enriched = enrichWithHistory(request, sessionId);
        SseEmitter emitter = new SseEmitter(300_000L);
        StringBuilder fullReply = new StringBuilder();
        CompletableFuture.runAsync(() -> {
            try {
                send(emitter, "session", Map.of("sessionId", sessionId));
                String resolvedAgentType = router.resolveAgentType(enriched);
                AgentTraceService.TraceBundle trace = traceService.collect(enriched, resolvedAgentType);
                send(emitter, "trace", Map.of("items", trace.items(), "agentType", resolvedAgentType));
                send(emitter, "reasoning", Map.of("content", trace.reasoning()));
                router.stream(enriched, delta -> {
                    fullReply.append(delta);
                    send(emitter, "delta", Map.of("content", delta));
                });
                // Save assistant reply
                if (hasText(request.getSessionId())) {
                    int turn = conversationService.nextTurnIndex(request.getAgentTypeOrDefault(), sessionId);
                    conversationService.saveMessage(request.getAgentTypeOrDefault(), sessionId, turn,
                            "assistant", fullReply.toString());
                }
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

    // ---- Helpers ----

    private AgentRequest enrichWithHistory(AgentRequest request, String sessionId) {
        if (!hasText(request.getSessionId())) return request;
        String agentType = request.getAgentTypeOrDefault();
        List<ConversationEntity> history = conversationService.loadRecentContext(agentType, sessionId, 100);
        if (!history.isEmpty()) {
            StringBuilder ctx = new StringBuilder();
            ctx.append("以下是之前的对话历史（共 ").append(history.size() / 2).append(" 轮），请记住上下文：\n\n");
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

    private String getOrCreateSession(AgentRequest request) {
        if (hasText(request.getSessionId())) return request.getSessionId();
        String newId = conversationService.newSessionId();
        request.setSessionId(newId);   // write back so saveMessage paths fire
        return newId;
    }

    private void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<Map<String, Object>> appendTraceCards(List<Map<String, Object>> cards, AgentTraceService.TraceBundle trace) {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        if (cards != null) result.addAll(cards);
        result.add(Map.of("type", "trace", "title", "工具调用", "items", trace.items()));
        result.add(Map.of("type", "reasoning", "title", "推理摘要", "content", trace.reasoning()));
        return result;
    }

    private boolean hasText(String s) { return s != null && !s.isBlank(); }
}

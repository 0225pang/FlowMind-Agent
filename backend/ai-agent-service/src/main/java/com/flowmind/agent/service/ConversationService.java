package com.flowmind.agent.service;

import com.flowmind.agent.entity.AgentSessionEntity;
import com.flowmind.agent.entity.ConversationEntity;
import com.flowmind.agent.mapper.ConversationMapper;
import com.flowmind.agent.mapper.SessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {
    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);
    private static final int MAX_TURNS = 100;

    private final ConversationMapper convMapper;
    private final SessionMapper sessionMapper;

    public ConversationService(ConversationMapper convMapper, SessionMapper sessionMapper) {
        this.convMapper = convMapper;
        this.sessionMapper = sessionMapper;
    }

    // ── Messages ──

    public List<ConversationEntity> loadHistory(String agentType, String sessionId) {
        return convMapper.loadAllTurns(agentType, sessionId);
    }

    public List<ConversationEntity> loadRecentContext(String agentType, String sessionId, int turns) {
        List<ConversationEntity> all = convMapper.loadAllTurns(agentType, sessionId);
        int msgCount = turns * 2;
        if (all.size() <= msgCount) return all;
        return all.subList(all.size() - msgCount, all.size());
    }

    public void saveMessage(String agentType, String sessionId, int turnIndex,
                             String role, String content) {
        convMapper.insert(agentType, sessionId, turnIndex, role, truncate(content, 8000));
        // Update session table: title from first user message
        if ("user".equals(role)) {
            String title = truncate(content, 80);
            sessionMapper.upsert(sessionId, agentType, title, turnIndex + 1);
        } else {
            sessionMapper.upsert(sessionId, agentType, null, turnIndex + 1);
        }
    }

    public int nextTurnIndex(String agentType, String sessionId) {
        List<ConversationEntity> recent = convMapper.loadAllTurns(agentType, sessionId);
        if (recent.isEmpty()) return 0;
        for (int i = recent.size() - 1; i >= 0; i--) {
            if ("assistant".equals(recent.get(i).getRole())) {
                return recent.get(i).getTurnIndex() + 1;
            }
        }
        return 0;
    }

    public void clearSession(String agentType, String sessionId) {
        convMapper.deleteSession(agentType, sessionId);
        sessionMapper.delete(sessionId);
    }

    public String newSessionId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // ── Sessions ──

    public List<AgentSessionEntity> listSessions() {
        return sessionMapper.listSessions();
    }

    public void deleteSession(String sessionId) {
        convMapper.deleteSession(null, sessionId);  // null = any agent type
        sessionMapper.delete(sessionId);
    }

    private String truncate(String s, int maxLen) {
        if (s == null || s.isEmpty()) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}

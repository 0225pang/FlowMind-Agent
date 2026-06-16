package com.flowmind.agent.entity;

import java.time.LocalDateTime;

public class ConversationEntity {
    private Long id;
    private String agentType;
    private String sessionId;
    private Integer turnIndex;
    private String role;       // "user" or "assistant"
    private String content;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAgentType() { return agentType; }
    public void setAgentType(String agentType) { this.agentType = agentType; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Integer getTurnIndex() { return turnIndex; }
    public void setTurnIndex(Integer turnIndex) { this.turnIndex = turnIndex; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

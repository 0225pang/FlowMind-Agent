package com.flowmind.agent.dto;

import java.util.Map;

public class AgentRequest {
    private String agentType;
    private String message;
    private String sessionId;
    private Map<String, Object> context;

    public String getAgentType() { return agentType; }
    public void setAgentType(String agentType) { this.agentType = agentType; }
    public String getAgentTypeOrDefault() { return agentType == null || agentType.isBlank() ? "content" : agentType.toLowerCase(); }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
}

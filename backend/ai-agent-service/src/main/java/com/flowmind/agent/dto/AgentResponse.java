package com.flowmind.agent.dto;

import java.util.*;

public class AgentResponse {
    private String agentType;
    private String reply;
    private String sessionId;
    private List<Map<String, Object>> cards;

    public AgentResponse() {}
    public AgentResponse(String agentType, String reply, List<Map<String, Object>> cards) {
        this.agentType = agentType; this.reply = reply; this.cards = cards;
    }
    public static AgentResponse of(String agentType, String reply, List<Map<String, Object>> cards) {
        return new AgentResponse(agentType, reply, cards);
    }

    public String getAgentType() { return agentType; }
    public void setAgentType(String agentType) { this.agentType = agentType; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public List<Map<String, Object>> getCards() { return cards; }
    public void setCards(List<Map<String, Object>> cards) { this.cards = cards; }
}

package com.flowmind.content.dto;

import java.util.List;
import java.util.Map;

public class ContentGenerateResponse {
    private String agentType;
    private List<String> sopSteps;
    private Map<String, Object> structureTemplate;
    private List<String> titles;
    private List<Map<String, Object>> versions;
    private List<String> tags;
    private Map<String, Object> persistence;

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public List<String> getSopSteps() {
        return sopSteps;
    }

    public void setSopSteps(List<String> sopSteps) {
        this.sopSteps = sopSteps;
    }

    public Map<String, Object> getStructureTemplate() {
        return structureTemplate;
    }

    public void setStructureTemplate(Map<String, Object> structureTemplate) {
        this.structureTemplate = structureTemplate;
    }

    public List<String> getTitles() {
        return titles;
    }

    public void setTitles(List<String> titles) {
        this.titles = titles;
    }

    public List<Map<String, Object>> getVersions() {
        return versions;
    }

    public void setVersions(List<Map<String, Object>> versions) {
        this.versions = versions;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getPersistence() {
        return persistence;
    }

    public void setPersistence(Map<String, Object> persistence) {
        this.persistence = persistence;
    }
}

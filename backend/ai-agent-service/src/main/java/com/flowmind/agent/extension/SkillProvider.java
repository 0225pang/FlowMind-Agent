package com.flowmind.agent.extension;

public interface SkillProvider extends AgentExtension {
    @Override
    default String type() {
        return "skill";
    }
}

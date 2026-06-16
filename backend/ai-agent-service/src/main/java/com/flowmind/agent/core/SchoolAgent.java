package com.flowmind.agent.core;

import com.flowmind.agent.dto.AgentRequest;
import com.flowmind.agent.dto.AgentResponse;
import com.flowmind.agent.extension.AgentExtension;
import com.flowmind.agent.llm.LLMClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SchoolAgent extends BaseAgent {
    public SchoolAgent(LLMClient llm, List<AgentExtension> extensions) {
        super(llm, extensions);
    }

    public String getName() {
        return "SchoolAgent";
    }

    public String getDescription() {
        return "院校项目整理、报名条件解析、材料要求梳理和学员匹配推荐助手。";
    }

    public AgentResponse execute(AgentRequest request) {
        return response("school", request.getMessage(), List.of(
                Map.of("title", "院校匹配", "content", "根据学员画像输出冲刺、匹配、保底项目"),
                Map.of("title", "项目情报", "content", "整理夏令营、预推免、报名条件、截止日期和材料要求"),
                Map.of("title", "飞书同步", "content", "后续可同步到飞书多维表格和任务提醒")
        ));
    }
}

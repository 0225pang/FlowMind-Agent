package com.flowmind.agent.core;

import com.flowmind.agent.dto.AgentRequest;
import com.flowmind.agent.dto.AgentResponse;
import com.flowmind.agent.extension.AgentExtension;
import com.flowmind.agent.llm.LLMClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class StudentAgent extends BaseAgent {
    public StudentAgent(LLMClient llm, List<AgentExtension> extensions) {
        super(llm, extensions);
    }

    public String getName() {
        return "StudentAgent";
    }

    public String getDescription() {
        return "学员画像、风险等级、申请建议、阶段进度和材料缺口分析助手。";
    }

    public AgentResponse execute(AgentRequest request) {
        return response("student", request.getMessage(), List.of(
                Map.of("title", "风险判断", "content", "综合 GPA、排名、英语、科研、目标院校和申请阶段"),
                Map.of("title", "行动建议", "content", "输出 1 周、1 月、申请季三个时间尺度的建议"),
                Map.of("title", "数据边界", "content", "不处理真实隐私，Demo 使用脱敏 mock 数据")
        ));
    }
}

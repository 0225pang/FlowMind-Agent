package com.flowmind.agent.core;

import com.flowmind.agent.dto.AgentRequest;
import com.flowmind.agent.dto.AgentResponse;
import com.flowmind.agent.extension.AgentExtension;
import com.flowmind.agent.llm.LLMClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ContentAgent extends BaseAgent {
    public ContentAgent(LLMClient llm, List<AgentExtension> extensions) {
        super(llm, extensions);
    }

    public String getName() {
        return "ContentAgent";
    }

    public String getDescription() {
        return "小红书爆款仿写、朋友圈人设表达、内容资产沉淀智能体。熟悉保研辅导、教育服务和个人 IP 内容运营场景。";
    }

    public AgentResponse execute(AgentRequest request) {
        return response("content", request.getMessage(), List.of(
                Map.of("title", "小红书内容智能体", "content", "爆款结构检索 -> 模板压缩 -> 三版笔记 -> 10 条标题 -> 飞书入库"),
                Map.of("title", "朋友圈内容智能体", "content", "场景识别 -> 人设映射 -> 三种风格 -> 真实感优化 -> 效果记录"),
                Map.of("title", "知识资产智能体", "content", "自动分类 -> 结构拆解 -> 模板库 -> 标签体系 -> 向量入库")
        ));
    }
}

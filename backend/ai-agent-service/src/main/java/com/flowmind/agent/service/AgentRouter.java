package com.flowmind.agent.service;

import com.flowmind.agent.core.Agent;
import com.flowmind.agent.dto.AgentRequest;
import com.flowmind.agent.dto.AgentResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class AgentRouter {
    private final Map<String, Agent> agents;

    public AgentRouter(List<Agent> list) {
        this.agents = list.stream().collect(Collectors.toMap(this::agentType, agent -> agent));
    }

    public List<Map<String, String>> listAgents() {
        List<Map<String, String>> result = new ArrayList<>();
        result.add(Map.of(
                "name", "FlowMindAgent",
                "type", "auto",
                "description", "Unified agent entry. It routes requests to content, knowledge, student, school or Feishu agents automatically."
        ));
        agents.values().stream()
                .map(agent -> Map.of(
                        "name", agent.getName(),
                        "type", agentType(agent),
                        "description", agent.getDescription()
                ))
                .forEach(result::add);
        return result;
    }

    public AgentResponse route(AgentRequest request) {
        Agent agent = resolve(request);
        AgentResponse response = agent.execute(request);
        response.setAgentType(agentType(agent));
        return response;
    }

    public void stream(AgentRequest request, Consumer<String> onDelta) {
        resolve(request).stream(request, onDelta);
    }

    private Agent resolve(AgentRequest request) {
        String inferred = inferAgentType(request);
        if (agents.containsKey(inferred)) return agents.get(inferred);

        String explicit = Optional.ofNullable(request == null ? null : request.getAgentType())
                .orElse("auto")
                .toLowerCase();
        if (!"auto".equals(explicit) && !"all".equals(explicit) && agents.containsKey(explicit)) {
            return agents.get(explicit);
        }
        return agents.getOrDefault("content", agents.values().iterator().next());
    }

    private String inferAgentType(AgentRequest request) {
        if (request == null || request.getMessage() == null) return "content";
        String text = request.getMessage().toLowerCase();

        if (containsAny(text, "保研知识库", "共享文件夹", "飞书文件夹")) return "feishu";
        if (isFeishuDocCreateRequest(text)) return "feishu";
        if (containsAny(text, "飞书", "lark", "feishu", "多维表格", "bitable", "base", "机器人", "群消息", "同步", "云文档")) return "feishu";
        if (containsAny(text, "学员", "学生", "gpa", "绩点", "排名", "英语", "四六级", "雅思", "托福", "画像", "风险", "申请阶段", "申请进度")) return "student";
        if (containsAny(text, "院校", "学校", "高校", "夏令营", "预推免", "推免", "报名条件", "截止", "材料要求", "推荐院校", "项目匹配")) return "school";
        if (containsAny(text, "知识库", "资料", "文档摘要", "总结文档", "提取标签", "标签", "rag", "向量", "检索", "模板库", "话术库", "sop")) return "knowledge";
        if (containsAny(text, "小红书", "朋友圈", "公众号", "选题", "文案", "标题", "内容日历", "爆款", "人设", "内容运营", "笔记", "转化")) return "content";
        return "content";
    }

    private boolean isFeishuDocCreateRequest(String text) {
        boolean mentionsDoc = containsAny(text, "飞书", "lark", "feishu", "文档", "doc", "云文档", "保研知识库");
        boolean createIntent = containsAny(text, "创建", "新建", "写一个", "写一篇", "写一份", "写上", "生成", "create");
        return mentionsDoc && createIntent;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword.toLowerCase())) return true;
        }
        return false;
    }

    private String agentType(Agent agent) {
        return agent.getName().replace("Agent", "").toLowerCase();
    }
}

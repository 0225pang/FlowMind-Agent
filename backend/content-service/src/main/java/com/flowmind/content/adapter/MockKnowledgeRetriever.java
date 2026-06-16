package com.flowmind.content.adapter;

import com.flowmind.content.dto.ContentGenerateRequest;
import com.flowmind.content.port.KnowledgeRetriever;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MockKnowledgeRetriever implements KnowledgeRetriever {
    @Override
    public List<Map<String, Object>> retrieveStructures(ContentGenerateRequest request) {
        return List.of(
                Map.of("source", "爆款结构库", "hook", "痛点 + 结果反差", "structure", "背景-方法-细节-避坑-引导"),
                Map.of("source", "朋友圈案例库", "hook", "真实场景 + 专业动作", "structure", "场景-动作-反馈-方法-克制收尾"),
                Map.of("source", "SOP模板库", "hook", "主题标签 + 转化目标", "structure", "分类-拆解-模板-标签-入库")
        );
    }
}

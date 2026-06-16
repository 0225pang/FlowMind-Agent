package com.flowmind.agent.controller;

import com.flowmind.common.core.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {
    private final List<Map<String, Object>> prompts = new CopyOnWriteArrayList<>(List.of(
            Map.of(
                    "id", 1,
                    "agentType", "content",
                    "name", "小红书爆款结构仿写",
                    "template", "围绕 {theme} 检索爆款结构，压缩为模板，并生成三版内容与 10 条标题。"
            ),
            Map.of(
                    "id", 2,
                    "agentType", "moments",
                    "name", "朋友圈人设表达",
                    "template", "根据 {scene} 和 {persona} 生成专业、可信、克制的朋友圈文案。"
            ),
            Map.of(
                    "id", 3,
                    "agentType", "student",
                    "name", "学员画像分析",
                    "template", "根据 GPA、排名、英语成绩、科研经历和目标院校生成风险等级与行动建议。"
            )
    ));

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.success(prompts);
    }

    @PostMapping
    public ApiResponse<?> add(@RequestBody Map<String, Object> body) {
        prompts.add(body);
        return ApiResponse.success(body);
    }
}

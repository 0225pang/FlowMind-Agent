package com.flowmind.content.adapter;

import com.flowmind.content.dto.ContentGenerateRequest;
import com.flowmind.content.port.ContentGenerationClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MockContentGenerationClient implements ContentGenerationClient {
    @Override
    public List<Map<String, Object>> generateVersions(ContentGenerateRequest request, Map<String, Object> template) {
        String topic = request.getTopic() == null ? "保研内容" : request.getTopic();
        if ("moments".equalsIgnoreCase(request.getAgentType())) {
            return List.of(
                    Map.of("name", "专业理性版", "content", "今天复盘了一次学员规划会，核心不是制造焦虑，而是把目标院校、材料缺口和下周动作拆清楚。"),
                    Map.of("name", "学姐温和版", "content", "陪同学一点点把申请节奏理顺，是这份工作里很有成就感的部分。很多不确定，其实都能被具体行动消化。"),
                    Map.of("name", "稍带传播版", "content", "一个好的保研规划，不是告诉你一定能上岸，而是让你知道每一步为什么做、怎么做、做到什么程度。")
            );
        }
        return List.of(
                Map.of("name", "干货版", "content", topic + "不要只写经历，要写清楚证据、动作和结果，让老师看到你的科研潜力。"),
                Map.of("name", "情绪增强版", "content", "很多同学焦虑不是因为经历少，而是不知道怎么把经历讲成优势。" + topic + "可以这样重写。"),
                Map.of("name", "转化引导版", "content", "如果你也卡在" + topic + "，可以先把成绩、科研、竞赛放进一张表，判断到底缺哪块。")
        );
    }

    @Override
    public List<String> generateTitles(ContentGenerateRequest request, Map<String, Object> template) {
        String topic = request.getTopic() == null ? "保研规划" : request.getTopic();
        return List.of(
                "保研er别再这样写" + topic,
                "靠这套方法，我把" + topic + "改顺了",
                topic + "最容易被忽略的 5 个细节",
                "普通本科生也能用的" + topic + "模板",
                "看完这篇再准备" + topic,
                topic + "：从混乱到清晰的完整流程",
                "导师真正想看的不是经历堆砌",
                "低年级一定要提前知道的" + topic,
                "保研申请里最该复盘的一页材料",
                "别让" + topic + "拖慢你的夏令营申请"
        );
    }
}

package com.flowmind.content.service;

import com.flowmind.content.dto.ContentGenerateRequest;
import com.flowmind.content.dto.ContentGenerateResponse;
import com.flowmind.content.port.BitableContentRepository;
import com.flowmind.content.port.ContentDocumentPublisher;
import com.flowmind.content.port.ContentGenerationClient;
import com.flowmind.content.port.KnowledgeRetriever;
import com.flowmind.content.port.LocalContentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ContentSopService {
    private final KnowledgeRetriever knowledgeRetriever;
    private final ContentGenerationClient generationClient;
    private final ContentDocumentPublisher documentPublisher;
    private final BitableContentRepository bitableRepository;
    private final LocalContentRepository localRepository;

    public ContentSopService(
            KnowledgeRetriever knowledgeRetriever,
            ContentGenerationClient generationClient,
            ContentDocumentPublisher documentPublisher,
            BitableContentRepository bitableRepository,
            LocalContentRepository localRepository
    ) {
        this.knowledgeRetriever = knowledgeRetriever;
        this.generationClient = generationClient;
        this.documentPublisher = documentPublisher;
        this.bitableRepository = bitableRepository;
        this.localRepository = localRepository;
    }

    public ContentGenerateResponse generateXiaohongshu(ContentGenerateRequest request) {
        request.setAgentType("xiaohongshu");
        return runPipeline(request, List.of(
                "爆款结构检索：模拟同主题 Top 20 高赞笔记结构",
                "结构压缩成模板：提取标题类型、开头钩子、正文结构、结尾转化",
                "内容生成：保留结构但替换案例、表达和逻辑顺序",
                "标题生成：焦虑型、结果型、对比型、数字型、经验总结型",
                "输出三版：干货版、情绪增强版、转化引导版",
                "自动入库：飞书多维表格、飞书文档、本地基础数据库"
        ));
    }

    public ContentGenerateResponse generateMoments(ContentGenerateRequest request) {
        request.setAgentType("moments");
        return runPipeline(request, List.of(
                "场景识别：成果型、过程型、思考型",
                "人设映射：专业感、可信度、克制情绪基调",
                "朋友圈结构：场景描述、专业动作、反馈、方法提炼、克制收尾",
                "生成三版：专业理性版、学姐温和版、稍带传播版",
                "自动优化：删除营销感词汇，增强真实细节，控制 100-300 字",
                "自动入库：场景标签、使用版本、转化效果记录"
        ));
    }

    public ContentGenerateResponse extractAsset(ContentGenerateRequest request) {
        request.setAgentType("asset");
        return runPipeline(request, List.of(
                "自动分类：小红书笔记、朋友圈内容、招生话术、方法论总结",
                "结构拆解：标题结构、开头模板、爆款钩子、转化话术",
                "模板沉淀：爆款标题模板、结构模板、朋友圈模板、招生话术库",
                "标签体系：主题、目的、风格、转化标签",
                "向量入库：支持后续相似主题检索和 RAG",
                "飞书沉淀：多维表格字段化管理并关联文档"
        ));
    }

    public Map<String, Object> architecture() {
        return Map.of(
                "trigger", List.of("飞书机器人", "前端界面"),
                "retrieval", List.of("向量数据库", "爆款结构库", "历史内容库"),
                "generation", List.of("LLM API", "Prompt 模板", "Agent Router"),
                "output", List.of("飞书文档", "本地基础数据库"),
                "database", List.of("飞书多维表格", "飞书文档", "本地基础数据库")
        );
    }

    private ContentGenerateResponse runPipeline(ContentGenerateRequest request, List<String> steps) {
        List<Map<String, Object>> retrievedStructures = knowledgeRetriever.retrieveStructures(request);
        Map<String, Object> template = compressTemplate(request, retrievedStructures);

        ContentGenerateResponse response = new ContentGenerateResponse();
        response.setAgentType(request.getAgentType());
        response.setSopSteps(steps);
        response.setStructureTemplate(template);
        response.setTitles(generationClient.generateTitles(request, template));
        response.setVersions(generationClient.generateVersions(request, template));
        response.setTags(List.of(
                safe(request.getTopic(), "保研内容"),
                safe(request.getStyle(), "干货"),
                request.getAgentType(),
                "可复用模板"
        ));

        String docToken = documentPublisher.publish(response);
        bitableRepository.saveStructuredRecord(response);
        response.setPersistence(Map.of(
                "feishuDoc", docToken,
                "feishuBitable", "mock-content-asset-table",
                "localDatabase", "content_theme/content_copy/content_calendar",
                "vectorDatabase", "reserved-vector-index"
        ));
        localRepository.save(response);
        return response;
    }

    private Map<String, Object> compressTemplate(ContentGenerateRequest request, List<Map<String, Object>> structures) {
        if ("moments".equalsIgnoreCase(request.getAgentType())) {
            return Map.of(
                    "sceneType", "过程型/成果型/思考型自动识别",
                    "persona", "专业、可信、克制",
                    "body", List.of("场景描述", "专业动作", "学员反馈", "方法提炼", "克制收尾"),
                    "retrieved", structures
            );
        }
        if ("asset".equalsIgnoreCase(request.getAgentType())) {
            return Map.of(
                    "categories", List.of("小红书笔记", "朋友圈内容", "招生话术", "方法论总结"),
                    "extractFields", List.of("标题结构", "开头模板", "爆款钩子", "转化话术"),
                    "tagSystem", List.of("主题", "目的", "风格", "转化标签"),
                    "retrieved", structures
            );
        }
        return Map.of(
                "title", "结果导向 + 人群 + 关键词",
                "opening", "痛点 + 身份 + 结果反差",
                "body", List.of("背景", "方法/步骤", "关键细节", "避坑总结"),
                "ending", "行动引导：评论/私信/关注",
                "retrieved", structures
        );
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

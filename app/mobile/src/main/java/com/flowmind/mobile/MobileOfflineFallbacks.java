package com.flowmind.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public final class MobileOfflineFallbacks {
    private MobileOfflineFallbacks() {
    }

    public static JSONArray themes() {
        JSONArray array = new JSONArray();
        put(array, obj("id", 1, "title", "保研简历怎么写才不像流水账", "summary", "围绕简历结构、项目表达和导师视角设计的小红书选题。", "channel", "小红书", "status", "待创作", "rating", 5));
        put(array, obj("id", 2, "title", "低 GPA 保研还有机会吗", "summary", "从科研、竞赛、推荐信和院校梯度拆解补强路径。", "channel", "朋友圈", "status", "已生成", "rating", 4));
        put(array, obj("id", 3, "title", "导师套磁邮件的三种常见误区", "summary", "适合转化为干货型笔记和咨询前置教育内容。", "channel", "公众号", "status", "待发布", "rating", 4));
        put(array, obj("id", 4, "title", "夏令营材料清单别等到最后一周", "summary", "用时间线制造行动提醒，适合朋友圈和社群推送。", "channel", "朋友圈", "status", "已发布", "rating", 5));
        put(array, obj("id", 5, "title", "普通本科食品专业如何定位院校", "summary", "和院校情报模块联动，适合引导用户进行项目匹配。", "channel", "小红书", "status", "待创作", "rating", 4));
        return array;
    }

    public static JSONArray drafts() {
        JSONArray array = new JSONArray();
        put(array, obj("id", 101, "themeId", 1, "title", "我会这样帮学员改保研简历", "content", "很多同学写简历时最容易把经历写成流水账。真正有效的写法，是先把项目拆成目标、动作、结果，再把和目标院校相关的能力放到前面。", "channel", "朋友圈", "usageStatus", "已使用", "publishDate", "2026-06-14", "rating", 5, "imageSuggestion", "书桌、简历批注、材料清单截图"));
        put(array, obj("id", 102, "themeId", 2, "title", "低 GPA 不是没机会，但不能再盲目投", "content", "低 GPA 的同学最怕的是只盯着分数焦虑，却没有补强策略。科研、竞赛、推荐信和院校梯度，其实都可以成为新的支点。", "channel", "小红书", "usageStatus", "未使用", "publishDate", "", "rating", 4, "imageSuggestion", "阶梯图、计划表、录取反馈打码图"));
        put(array, obj("id", 103, "themeId", 3, "title", "导师套磁邮件不要一上来就求机会", "content", "套磁不是群发模板，而是一次专业沟通。先说明研究兴趣，再连接导师方向，最后提出一个具体、克制的问题。", "channel", "公众号", "usageStatus", "未使用", "publishDate", "", "rating", 4, "imageSuggestion", "邮件结构示意图"));
        return array;
    }

    public static JSONArray calendar() {
        JSONArray array = new JSONArray();
        put(array, obj("id", 1, "draftId", 101, "themeId", 1, "title", "我会这样帮学员改保研简历", "publishDate", "2026-06-14", "channel", "朋友圈", "status", "已发布", "usageStatus", "已使用"));
        put(array, obj("id", 2, "draftId", 102, "themeId", 2, "title", "低 GPA 不是没机会，但不能再盲目投", "publishDate", "2026-06-18", "channel", "小红书", "status", "待发布", "usageStatus", "未使用"));
        put(array, obj("id", 3, "draftId", 103, "themeId", 3, "title", "导师套磁邮件不要一上来就求机会", "publishDate", "2026-06-25", "channel", "公众号", "status", "待发布", "usageStatus", "未使用"));
        return array;
    }

    public static JSONArray knowledgeDocs() {
        JSONArray array = new JSONArray();
        put(array, obj("id", 1, "title", "期末如何速成课程论文", "summary", "包含选题缩小、文献综述、结构搭建和引用规范。", "category", "课程论文", "source", "飞书知识库"));
        put(array, obj("id", 2, "title", "保研简历材料清单", "summary", "覆盖个人信息、教育经历、科研项目、竞赛奖项和推荐信。", "category", "申请材料", "source", "本地知识库"));
        put(array, obj("id", 3, "title", "夏令营时间线", "summary", "按月份拆解报名、材料、面试和确认节点。", "category", "院校情报", "source", "飞书知识库"));
        return array;
    }

    public static JSONArray vectorHits() {
        JSONArray array = new JSONArray();
        put(array, obj("title", "期末如何速成课程论文", "chunkText", "先把题目缩小到一个可论证的问题，再用 6-8 篇核心文献搭建综述，正文按问题、分析、结论展开。", "source", "保研知识库", "distance", 0.18));
        put(array, obj("title", "课程论文文献综述模板", "chunkText", "文献综述不要简单罗列，应按观点分组，说明已有研究如何支持你的论文问题。", "source", "保研知识库", "distance", 0.24));
        return array;
    }

    public static JSONArray schools() {
        JSONArray array = new JSONArray();
        put(array, obj("id", 1, "name", "中国农业大学", "region", "北京", "level", "985", "disciplineTags", "食品,生物,农业"));
        put(array, obj("id", 2, "name", "江南大学", "region", "无锡", "level", "211", "disciplineTags", "食品,发酵,轻工"));
        put(array, obj("id", 3, "name", "华南理工大学", "region", "广州", "level", "985", "disciplineTags", "食品,材料,化工"));
        return array;
    }

    public static JSONArray projects() {
        JSONArray array = new JSONArray();
        put(array, obj("id", 1, "schoolName", "中国农业大学", "projectName", "食品科学与营养工程学院夏令营", "projectType", "夏令营", "deadline", "2026-07-05", "requirements", "专业排名前 30%，英语六级 500+，有科研经历优先。", "materials", "简历、成绩单、排名证明、英语证明、个人陈述", "matchScore", 88));
        put(array, obj("id", 2, "schoolName", "江南大学", "projectName", "食品学院优秀大学生夏令营", "projectType", "夏令营", "deadline", "2026-07-18", "requirements", "食品相关专业，成绩优秀，对科研有明确兴趣。", "materials", "申请表、成绩单、获奖证明、科研材料", "matchScore", 84));
        put(array, obj("id", 3, "schoolName", "华南理工大学", "projectName", "轻工与食品预推免项目", "projectType", "预推免", "deadline", "2026-09-10", "requirements", "本科成绩优秀，有项目经历或论文经历优先。", "materials", "简历、个人陈述、推荐信、科研证明", "matchScore", 81));
        return array;
    }

    public static JSONObject feishuStatus() {
        return obj("available", true, "mode", "mock-or-cli", "folderToken", "KELsfW0jvlHcVqdiuTncQ66Lnnc", "message", "离线兜底：后端不可用时展示，真实状态以接口为准。");
    }

    public static JSONObject overview() {
        JSONObject json = new JSONObject();
        try {
            json.put("themes", themes().length());
            json.put("drafts", drafts().length());
            json.put("knowledgeDocs", knowledgeDocs().length());
            json.put("schools", schools().length());
            json.put("projects", projects().length());
            json.put("calendarItems", calendar().length());
            json.put("note", "这些数据仅用于移动端离线兜底展示，联网成功后以真实后端接口为准。");
        } catch (Exception ignored) {
        }
        return json;
    }

    public static JSONArray byScene(String route) {
        if (MobileFeatureCatalog.ROUTE_CONTENT.equals(route)) return themes();
        if (MobileFeatureCatalog.ROUTE_KNOWLEDGE.equals(route)) return knowledgeDocs();
        if (MobileFeatureCatalog.ROUTE_SCHOOLS.equals(route)) return projects();
        return new JSONArray();
    }

    public static JSONObject obj(Object... pairs) {
        JSONObject json = new JSONObject();
        try {
            for (int i = 0; i + 1 < pairs.length; i += 2) {
                json.put(String.valueOf(pairs[i]), pairs[i + 1]);
            }
        } catch (Exception ignored) {
        }
        return json;
    }

    private static void put(JSONArray array, JSONObject object) {
        array.put(object);
    }

    public static List<String> offlineReasons() {
        return List.of(
                "手机无法访问 ngrok 或后端服务",
                "后端正在重启，接口暂时不可用",
                "用户角色没有访问某个接口的权限",
                "Weaviate、飞书 CLI 或 MySQL 暂时不可用",
                "课堂答辩时需要先展示 UI，再等待网络恢复"
        );
    }

    public static List<String> offlineWarnings() {
        return List.of(
                "离线兜底数据不能写回数据库",
                "AI 工作台不能在离线模式下生成真实回复",
                "飞书文档创建必须等待后端和 lark-cli 可用",
                "向量检索离线结果只用于展示，不代表真实召回"
        );
    }
}

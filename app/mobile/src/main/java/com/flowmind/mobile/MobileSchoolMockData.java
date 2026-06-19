package com.flowmind.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Rich mobile fallback data for the school intelligence scene.
 *
 * The backend remains the preferred data source. These records keep the mobile
 * demo useful during presentations when the backend tunnel, MySQL or school
 * service is temporarily unavailable. Fields intentionally mirror common API
 * response names so the same rendering code can handle real and mock records.
 */
public final class MobileSchoolMockData {
    private MobileSchoolMockData() {
    }

    public static JSONArray schools() {
        JSONArray array = new JSONArray();
        add(array, school(
                1,
                "中国农业大学",
                "北京",
                "985 / 双一流",
                "食品科学与营养工程学院",
                "食品科学、营养健康、农产品加工",
                "适合食品、生物、农学交叉背景学生，科研经历和英语成绩权重较高。",
                "A",
                92
        ));
        add(array, school(
                2,
                "江南大学",
                "无锡",
                "211 / 双一流",
                "食品学院",
                "食品科学、发酵工程、轻工技术",
                "食品方向优势明显，对食品专业本科生友好，材料完整度很关键。",
                "A",
                90
        ));
        add(array, school(
                3,
                "华南理工大学",
                "广州",
                "985 / 双一流",
                "轻工科学与工程学院",
                "轻工、食品、材料与化工",
                "适合有项目经历、竞赛经历或跨学科研究兴趣的学生。",
                "A-",
                86
        ));
        add(array, school(
                4,
                "南京农业大学",
                "南京",
                "211 / 双一流",
                "食品科技学院",
                "食品质量安全、农产品加工、微生物",
                "项目梯度适中，适合作为稳妥目标和重点跟进院校。",
                "B+",
                82
        ));
        add(array, school(
                5,
                "浙江大学",
                "杭州",
                "985 / 双一流",
                "生物系统工程与食品科学学院",
                "食品工程、生物系统、智能农业",
                "竞争强度高，建议科研成果、排名和推荐信都较强时冲刺。",
                "A+",
                95
        ));
        add(array, school(
                6,
                "上海交通大学",
                "上海",
                "985 / 双一流",
                "农业与生物学院",
                "食品、生物、农业工程",
                "偏综合能力评估，适合英语和科研表达能力较强的学生。",
                "A",
                93
        ));
        return array;
    }

    public static JSONArray projects() {
        JSONArray array = new JSONArray();
        add(array, project(
                101,
                "中国农业大学",
                "食品科学与营养工程学院优秀大学生夏令营",
                "夏令营",
                "2026-07-05",
                "专业排名前 30%；英语六级 500+；有科研、竞赛、论文或开放实验经历优先。",
                "申请表、成绩单、排名证明、英语证明、个人陈述、专家推荐信、获奖证明。",
                "食品科学 / 营养健康 / 农产品加工",
                "高",
                88,
                "适合排名靠前、科研经历较完整的食品专业学生。"
        ));
        add(array, project(
                102,
                "江南大学",
                "食品学院全国优秀大学生暑期学校",
                "夏令营",
                "2026-07-18",
                "食品相关专业；成绩优秀；对食品科学研究有明确兴趣；鼓励提交科研训练材料。",
                "申请表、成绩单、获奖证书、科研材料、英语成绩、个人简历。",
                "食品科学 / 发酵工程 / 食品安全",
                "中高",
                84,
                "食品方向匹配度高，适合作为重点稳妥项目。"
        ));
        add(array, project(
                103,
                "华南理工大学",
                "轻工与食品预推免项目",
                "预推免",
                "2026-09-10",
                "本科成绩优秀；有项目经历、论文经历或竞赛经历优先；专业方向需与导师方向匹配。",
                "简历、个人陈述、推荐信、成绩排名证明、科研证明、英语证明。",
                "食品工程 / 轻工技术 / 材料化工",
                "中",
                81,
                "适合有项目表达能力、想做交叉方向的学生。"
        ));
        add(array, project(
                104,
                "南京农业大学",
                "食品科技学院推免生预报名",
                "预推免",
                "2026-08-28",
                "本科阶段成绩良好；食品、农产品加工、生物相关方向；有实验室经历优先。",
                "预报名表、成绩单、个人简历、英语证明、科研或竞赛支撑材料。",
                "农产品加工 / 食品质量安全 / 微生物",
                "中",
                79,
                "适合需要补充稳妥梯度的学生，材料准备周期相对友好。"
        ));
        add(array, project(
                105,
                "浙江大学",
                "食品与生物系统方向优秀大学生项目",
                "夏令营",
                "2026-06-30",
                "专业排名靠前；英语能力突出；有高质量科研项目、论文、竞赛或创新训练经历。",
                "申请表、成绩单、排名证明、英语成绩、个人陈述、两封推荐信、科研成果证明。",
                "食品工程 / 生物系统 / 智能农业",
                "很高",
                76,
                "冲刺项目，建议只推荐给综合材料非常完整的学生。"
        ));
        add(array, project(
                106,
                "上海交通大学",
                "农业与生物学院优秀大学生夏令营",
                "夏令营",
                "2026-07-12",
                "成绩优秀；英语沟通能力较好；有食品、生物或农业工程相关科研训练经历。",
                "报名表、成绩单、英语证明、个人陈述、科研证明、推荐信。",
                "食品 / 生物 / 农业工程",
                "高",
                83,
                "适合想冲综合强校、表达能力较强的学生。"
        ));
        add(array, project(
                107,
                "中国海洋大学",
                "食品科学与工程学院夏令营",
                "夏令营",
                "2026-07-22",
                "食品、水产、海洋生物相关专业；成绩优秀；有实验或创新训练经历更佳。",
                "申请表、成绩单、排名证明、个人简历、英语证明、获奖证书。",
                "水产品加工 / 食品安全 / 海洋生物资源",
                "中",
                78,
                "适合食品与海洋资源方向兴趣明确的学生。"
        ));
        add(array, project(
                108,
                "西北农林科技大学",
                "食品科学与工程学院推免生开放日",
                "开放日",
                "2026-08-15",
                "食品、生物、农学相关专业；本科成绩良好；有科研兴趣和继续深造意愿。",
                "简历、成绩单、英语成绩、个人陈述、获奖证明。",
                "食品加工 / 农产品贮藏 / 功能食品",
                "中低",
                74,
                "适合作为保底和区域匹配项目。"
        ));
        return array;
    }

    public static JSONArray deadlineTrend() {
        JSONArray array = new JSONArray();
        add(array, item("month", "6月", "count", 1, "risk", "high"));
        add(array, item("month", "7月", "count", 4, "risk", "high"));
        add(array, item("month", "8月", "count", 2, "risk", "medium"));
        add(array, item("month", "9月", "count", 1, "risk", "medium"));
        return array;
    }

    public static JSONObject recommendation(String profile) {
        JSONObject result = new JSONObject();
        JSONArray picks = new JSONArray();
        add(picks, item(
                "schoolName", "江南大学",
                "projectName", "食品学院全国优秀大学生暑期学校",
                "matchScore", 88,
                "reason", "食品方向匹配度高，项目梯度适中，适合有科研训练但需要稳妥上岸的学生。",
                "risk", "中",
                "nextAction", "优先整理食品科研经历和个人陈述中的研究兴趣。"
        ));
        add(picks, item(
                "schoolName", "中国农业大学",
                "projectName", "食品科学与营养工程学院优秀大学生夏令营",
                "matchScore", 84,
                "reason", "院校层次高，食品与营养方向强，但对排名和英语成绩要求更高。",
                "risk", "中高",
                "nextAction", "补齐排名证明、英语证明和推荐信，突出科研产出。"
        ));
        add(picks, item(
                "schoolName", "南京农业大学",
                "projectName", "食品科技学院推免生预报名",
                "matchScore", 80,
                "reason", "梯度相对稳妥，适合作为保底和稳妥项目。",
                "risk", "中低",
                "nextAction", "提前准备材料，关注 8 月底预报名截止。"
        ));
        try {
            result.put("profile", profile == null ? "" : profile);
            result.put("strategy", "建议按照“冲刺 2 个、重点 3 个、稳妥 2 个”的梯度准备材料。");
            result.put("picks", picks);
            result.put("warning", "Mock 推荐仅用于移动端离线演示，正式匹配应以后台接口和真实项目数据为准。");
        } catch (Exception ignored) {
        }
        return result;
    }

    private static JSONObject school(int id, String name, String region, String level, String college, String tags, String summary, String rank, int heat) {
        return item(
                "id", id,
                "name", name,
                "schoolName", name,
                "region", region,
                "location", region,
                "level", level,
                "college", college,
                "disciplineTags", tags,
                "summary", summary,
                "rankLevel", rank,
                "heat", heat
        );
    }

    private static JSONObject project(int id, String school, String project, String type, String deadline, String requirements, String materials, String tags, String competition, int score, String advice) {
        return item(
                "id", id,
                "schoolName", school,
                "name", school,
                "projectName", project,
                "title", project,
                "projectType", type,
                "deadline", deadline,
                "requirements", requirements,
                "materials", materials,
                "disciplineTags", tags,
                "competitionLevel", competition,
                "matchScore", score,
                "advice", advice,
                "status", deadline.compareTo("2026-07-10") <= 0 ? "临近截止" : "可准备"
        );
    }

    private static JSONObject item(Object... pairs) {
        JSONObject json = new JSONObject();
        try {
            for (int i = 0; i + 1 < pairs.length; i += 2) {
                json.put(String.valueOf(pairs[i]), pairs[i + 1]);
            }
        } catch (Exception ignored) {
        }
        return json;
    }

    private static void add(JSONArray array, JSONObject object) {
        array.put(object);
    }
}

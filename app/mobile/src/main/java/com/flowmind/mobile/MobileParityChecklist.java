package com.flowmind.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MobileParityChecklist {
    private MobileParityChecklist() {
    }

    public static List<CheckGroup> groups() {
        List<CheckGroup> groups = new ArrayList<>();
        groups.add(agentGroup());
        groups.add(knowledgeGroup());
        groups.add(contentGroup());
        groups.add(schoolGroup());
        groups.add(settingsGroup());
        groups.add(safetyGroup());
        return groups;
    }

    public static CheckGroup agentGroup() {
        return new CheckGroup("AI 工作台", MobileFeatureCatalog.ROUTE_AGENT, List.of(
                item("总智能体入口", "默认 auto agent，不需要用户手动选择单个 Agent", true),
                item("SSE 流式回复", "解析 delta、done、error 事件", true),
                item("工具调用展示", "只展示实际调用的工具，skipped 不展示", true),
                item("Thinking 展示", "流式显示预览，完成后可展开详情", true),
                item("快捷指令", "知识库总结、生成选题、院校推荐、飞书能力", true),
                item("Markdown 渲染", "标题、加粗、列表、代码片段基础解析", true),
                item("会话恢复", "保留 sessionId，后续可扩展会话列表", false),
                item("错误防呆", "网络失败时给出 Base URL、ngrok、后端日志提示", true)
        ));
    }

    public static CheckGroup knowledgeGroup() {
        return new CheckGroup("知识库", MobileFeatureCatalog.ROUTE_KNOWLEDGE, List.of(
                item("知识库统计", "展示文档、标签、向量状态", true),
                item("文档列表", "卡片展示文档标题、摘要、来源", true),
                item("向量检索", "输入关键词调用 /api/knowledge/vector/search", true),
                item("详情弹窗", "点击文档或片段查看原始 JSON/正文", true),
                item("空结果提示", "无结果时提示换关键词和检查 Weaviate", true),
                item("同步入口", "后续可从手机端触发飞书同步", false),
                item("标签过滤", "后续可对齐 Web 的标签筛选", false)
        ));
    }

    public static CheckGroup contentGroup() {
        return new CheckGroup("内容创作", MobileFeatureCatalog.ROUTE_CONTENT, List.of(
                item("主题库", "展示所有主题，卡片含评分、状态、热度", true),
                item("主题详情", "点击卡片弹出详情", true),
                item("主题评分", "五星评分调用后端接口", true),
                item("历史文案", "从主题详情查看该主题下生成的历史文案", true),
                item("文案库", "展示所有文案，不只展示主题下文案", true),
                item("文案详情", "展示正文、使用状态、发布时间", true),
                item("文案编辑", "弹窗编辑标题和正文", true),
                item("图片登记", "可保存图片 URL，没有图时展示配图建议", true),
                item("内容日历", "有发布内容的日期绘制标记", true),
                item("日期展开列表", "日历下方展示对应发布列表", true),
                item("手动新增", "后续可补移动端新增主题/文案表单", false)
        ));
    }

    public static CheckGroup schoolGroup() {
        return new CheckGroup("院校情报", MobileFeatureCatalog.ROUTE_SCHOOLS, List.of(
                item("学校列表", "展示学校名称、地区、层次、学科标签", true),
                item("项目列表", "展示夏令营/预推免项目、截止日期、条件", true),
                item("项目详情", "点击查看材料要求和原始数据", true),
                item("趋势图", "移动端使用原生 Canvas 迷你柱状图", true),
                item("AI 推荐", "输入学生画像调用 /api/schools/recommend", true),
                item("画像防呆", "缺 GPA/排名/英语时给出补全提示", true),
                item("截止提醒", "后续可接系统通知或日历", false)
        ));
    }

    public static CheckGroup settingsGroup() {
        return new CheckGroup("设置", MobileFeatureCatalog.ROUTE_SETTINGS, List.of(
                item("Base URL 配置", "支持 ngrok 或本地后端地址", true),
                item("当前用户", "读取 /api/users/me 并展示角色权限", true),
                item("权限提示", "无权限页面提示联系管理员", true),
                item("飞书状态", "检查飞书同步状态", true),
                item("退出登录", "清理本地 token 和登录状态", true),
                item("诊断信息", "后续可展示状态快照和操作日志", false)
        ));
    }

    public static CheckGroup safetyGroup() {
        return new CheckGroup("统一防呆", "/safety", List.of(
                item("请求失败提示", "401/403/404/500/超时/ngrok 分别给出解释", true),
                item("空数据状态", "每个场景有明确空状态和下一步建议", true),
                item("权限防呆", "前端置灰，后端拦截", true),
                item("输入校验", "Prompt、标题、图片 URL、学生画像均校验", true),
                item("敏感信息提示", "Prompt 中疑似 API Key 时提醒", true),
                item("操作日志", "记录请求、成功、失败和耗时", true),
                item("缓存快照", "保留最近结果，后续可离线展示", false)
        ));
    }

    public static ChecklistItem item(String name, String description, boolean complete) {
        return new ChecklistItem(name, description, complete);
    }

    public static JSONObject summary() {
        JSONObject json = new JSONObject();
        JSONArray groupArray = new JSONArray();
        int total = 0;
        int complete = 0;
        try {
            for (CheckGroup group : groups()) {
                groupArray.put(group.toJson());
                total += group.items.size();
                complete += group.completedCount();
            }
            json.put("total", total);
            json.put("complete", complete);
            json.put("percent", total == 0 ? 0 : Math.round(complete * 100.0 / total));
            json.put("groups", groupArray);
        } catch (Exception ignored) {
        }
        return json;
    }

    public static List<String> incompleteItems() {
        List<String> list = new ArrayList<>();
        for (CheckGroup group : groups()) {
            for (ChecklistItem item : group.items) {
                if (!item.complete) list.add(group.title + " - " + item.name + "：" + item.description);
            }
        }
        return list;
    }

    public static Map<String, Integer> completionMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (CheckGroup group : groups()) {
            map.put(group.title, group.percent());
        }
        return map;
    }

    public static final class CheckGroup {
        public final String title;
        public final String route;
        public final List<ChecklistItem> items;

        public CheckGroup(String title, String route, List<ChecklistItem> items) {
            this.title = title;
            this.route = route;
            this.items = items;
        }

        public int completedCount() {
            int count = 0;
            for (ChecklistItem item : items) if (item.complete) count++;
            return count;
        }

        public int percent() {
            if (items.isEmpty()) return 0;
            return (int) Math.round(completedCount() * 100.0 / items.size());
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            JSONArray array = new JSONArray();
            try {
                for (ChecklistItem item : items) array.put(item.toJson());
                json.put("title", title);
                json.put("route", route);
                json.put("complete", completedCount());
                json.put("total", items.size());
                json.put("percent", percent());
                json.put("items", array);
            } catch (Exception ignored) {
            }
            return json;
        }
    }

    public static final class ChecklistItem {
        public final String name;
        public final String description;
        public final boolean complete;

        public ChecklistItem(String name, String description, boolean complete) {
            this.name = name;
            this.description = description;
            this.complete = complete;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("name", name);
                json.put("description", description);
                json.put("complete", complete);
            } catch (Exception ignored) {
            }
            return json;
        }
    }
}

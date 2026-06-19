package com.flowmind.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MobileScreenBlueprints {
    private MobileScreenBlueprints() {
    }

    public static List<ScreenBlueprint> all() {
        return List.of(agent(), knowledge(), content(), school(), settings());
    }

    public static ScreenBlueprint agent() {
        ScreenBlueprint blueprint = new ScreenBlueprint("agent", "AI 工作台", MobileFeatureCatalog.ROUTE_AGENT);
        blueprint.addSection("顶部状态", "展示总智能体状态、当前工作空间、角色标签", true)
                .addSection("快捷指令", "与 Web 端保持一致，覆盖知识库总结、选题生成、飞书状态、院校推荐", true)
                .addSection("对话流", "用户消息右侧气泡，AI 消息左侧气泡，支持 Markdown 基础解析", true)
                .addSection("工具调用", "只展示实际调用工具，点击可展开原始 trace", true)
                .addSection("模型 Thinking", "流式时显示预览，完成后点击展开完整内容", true)
                .addSection("输入区", "多行输入、发送防抖、敏感信息提醒、超长文本阻止", true)
                .addSection("会话列表", "后续与 Web ConversationHistory 对齐", false);
        blueprint.addAction("发送消息", "validatePrompt -> postSse -> append delta -> done", "POST /api/agents/chat/stream")
                .addAction("查看工具详情", "trace event -> dialog json", "SSE trace")
                .addAction("展开 Thinking", "reasoning buffer -> dialog markdown", "SSE reasoning")
                .addAction("新建会话", "clear local messages and request new session", "POST /api/agents/sessions");
        blueprint.addGuard("Prompt 为空时禁止发送")
                .addGuard("Prompt 疑似包含 API Key 时警告")
                .addGuard("SSE 失败时展示后端地址、ngrok、模型配置排查项")
                .addGuard("无 AI 权限时底部 Tab 置灰");
        return blueprint;
    }

    public static ScreenBlueprint knowledge() {
        ScreenBlueprint blueprint = new ScreenBlueprint("knowledge", "知识库", MobileFeatureCatalog.ROUTE_KNOWLEDGE);
        blueprint.addSection("统计卡片", "文档数、标签数、向量状态，与 Web 顶部统计一致", true)
                .addSection("语义检索", "输入关键词检索 Weaviate，结果展示标题、片段、来源、相关度", true)
                .addSection("文档列表", "展示知识库文档，点击查看详情", true)
                .addSection("标签体系", "后续补充标签筛选，和 Web 标签栏一致", false)
                .addSection("同步入口", "后续移动端触发飞书同步", false);
        blueprint.addAction("检索知识", "validateKeyword -> GET vector search -> render hits", "GET /api/knowledge/vector/search")
                .addAction("加载文档", "GET docs -> map KnowledgeDoc -> cards", "GET /api/knowledge/docs")
                .addAction("查看详情", "card click -> dialog json/markdown", "local")
                .addAction("同步知识库", "confirm -> POST sync -> refresh", "POST /api/knowledge/sync");
        blueprint.addGuard("关键词为空时阻止")
                .addGuard("检索结果为空时提示检查 Weaviate")
                .addGuard("接口 403 时提示角色无知识库权限")
                .addGuard("返回 HTML 时提示 ngrok warning header");
        return blueprint;
    }

    public static ScreenBlueprint content() {
        ScreenBlueprint blueprint = new ScreenBlueprint("content", "内容创作", MobileFeatureCatalog.ROUTE_CONTENT);
        blueprint.addSection("内容概览", "主题数、文案数、待发布数，与 Web 内容运营顶部一致", true)
                .addSection("主题库", "3x2 Web 设计在移动端转为纵向卡片流，保留评分和状态", true)
                .addSection("文案库", "展示所有文案，可查看使用状态和发布时间", true)
                .addSection("内容日历", "Canvas 绘制日期格，有内容发布的日期加标记", true)
                .addSection("历史文案", "主题详情中进入该主题历史文案列表", true)
                .addSection("图片管理", "有图展示图片引用，没有图展示配图建议", true)
                .addSection("手动新增", "后续补充新增主题/文案表单", false);
        blueprint.addAction("刷新主题", "GET themes -> map ContentTheme -> render", "GET /api/content/themes")
                .addAction("查看主题", "theme card -> detail dialog", "local")
                .addAction("主题评分", "validateRating -> PUT rating", "PUT /api/content/themes/{id}/rating")
                .addAction("加载历史文案", "GET theme drafts -> render drafts", "GET /api/content/themes/{themeId}/drafts")
                .addAction("编辑文案", "validateTitle/copy -> PUT draft", "PUT /api/content/drafts/{draftId}")
                .addAction("登记图片", "validateImageUrl -> POST image", "POST /api/content/drafts/{draftId}/images")
                .addAction("查看日历", "GET calendar -> Canvas board + publish list", "GET /api/content/calendar");
        blueprint.addGuard("评分必须 1-5 星")
                .addGuard("标题和文案不能为空")
                .addGuard("图片 URL 必须 http/https")
                .addGuard("没有图片时保留配图建议，避免空白")
                .addGuard("删除/修改类操作需要确认")
                .addGuard("学生角色隐藏内容创作 Tab");
        return blueprint;
    }

    public static ScreenBlueprint school() {
        ScreenBlueprint blueprint = new ScreenBlueprint("school", "院校情报", MobileFeatureCatalog.ROUTE_SCHOOLS);
        blueprint.addSection("趋势图", "原生 Canvas 柱状图展示截止趋势", true)
                .addSection("学校列表", "展示学校名称、地区、层次和标签", true)
                .addSection("项目列表", "展示项目名称、学校、截止日期、条件、材料", true)
                .addSection("AI 推荐", "输入学生画像匹配项目", true)
                .addSection("截止提醒", "后续可接日历/通知", false);
        blueprint.addAction("加载学校", "GET schools -> map SchoolInfo -> cards", "GET /api/schools")
                .addAction("加载项目", "GET projects -> map SchoolProject -> cards", "GET /api/school-projects")
                .addAction("项目详情", "project card -> dialog", "local")
                .addAction("匹配推荐", "validateStudentProfile -> POST recommend", "POST /api/schools/recommend");
        blueprint.addGuard("画像缺少 GPA/排名/英语时提示补全")
                .addGuard("项目列表为空时提示检查 Mock 数据")
                .addGuard("无院校权限时允许去知识库/AI 工作台")
                .addGuard("截止日期字段异常时仍展示项目卡片");
        return blueprint;
    }

    public static ScreenBlueprint settings() {
        ScreenBlueprint blueprint = new ScreenBlueprint("settings", "设置", MobileFeatureCatalog.ROUTE_SETTINGS);
        blueprint.addSection("Base URL", "修改后端地址，自动去掉末尾斜杠", true)
                .addSection("当前用户", "展示用户名、角色、权限数量", true)
                .addSection("权限诊断", "列出可访问页面和被限制页面", true)
                .addSection("飞书状态", "检查飞书同步状态", true)
                .addSection("操作日志", "展示最近请求和失败原因", false)
                .addSection("退出登录", "清理本地 token 和状态", true);
        blueprint.addAction("保存后端地址", "validateBaseUrl -> persist prefs", "local")
                .addAction("检查用户", "GET me -> map UserProfile", "GET /api/users/me")
                .addAction("检查飞书", "GET feishu status", "GET /api/feishu/sync/status")
                .addAction("退出登录", "confirm if streaming -> clear prefs", "local");
        blueprint.addGuard("Base URL 为空时禁止保存")
                .addGuard("退出时若有流式请求提醒")
                .addGuard("401 时自动提示重新登录")
                .addGuard("403 时展示当前角色和所需权限");
        return blueprint;
    }

    public static JSONObject toJson() {
        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();
        try {
            for (ScreenBlueprint item : all()) array.put(item.toJson());
            json.put("screens", array);
            json.put("totalSections", all().stream().mapToInt(screen -> screen.sections.size()).sum());
            json.put("totalActions", all().stream().mapToInt(screen -> screen.actions.size()).sum());
            json.put("totalGuards", all().stream().mapToInt(screen -> screen.guards.size()).sum());
        } catch (Exception ignored) {
        }
        return json;
    }

    public static List<String> implementationHints(String key) {
        ScreenBlueprint blueprint = find(key);
        if (blueprint == null) return List.of("没有找到页面蓝图");
        List<String> hints = new ArrayList<>();
        hints.add("页面：" + blueprint.title);
        for (ScreenSection section : blueprint.sections) {
            hints.add((section.done ? "[已完成] " : "[待补充] ") + section.name + " - " + section.description);
        }
        for (ScreenAction action : blueprint.actions) {
            hints.add("操作：" + action.name + " -> " + action.flow + " -> " + action.endpoint);
        }
        for (String guard : blueprint.guards) {
            hints.add("防呆：" + guard);
        }
        return hints;
    }

    public static ScreenBlueprint find(String key) {
        for (ScreenBlueprint blueprint : all()) {
            if (blueprint.key.equals(key) || blueprint.route.equals(key)) return blueprint;
        }
        return null;
    }

    public static final class ScreenBlueprint {
        public final String key;
        public final String title;
        public final String route;
        public final List<ScreenSection> sections = new ArrayList<>();
        public final List<ScreenAction> actions = new ArrayList<>();
        public final List<String> guards = new ArrayList<>();

        public ScreenBlueprint(String key, String title, String route) {
            this.key = key;
            this.title = title;
            this.route = route;
        }

        public ScreenBlueprint addSection(String name, String description, boolean done) {
            sections.add(new ScreenSection(name, description, done));
            return this;
        }

        public ScreenBlueprint addAction(String name, String flow, String endpoint) {
            actions.add(new ScreenAction(name, flow, endpoint));
            return this;
        }

        public ScreenBlueprint addGuard(String guard) {
            guards.add(guard);
            return this;
        }

        public int completionPercent() {
            if (sections.isEmpty()) return 0;
            int done = 0;
            for (ScreenSection section : sections) if (section.done) done++;
            return (int) Math.round(done * 100.0 / sections.size());
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            JSONArray sectionArray = new JSONArray();
            JSONArray actionArray = new JSONArray();
            JSONArray guardArray = new JSONArray();
            try {
                for (ScreenSection section : sections) sectionArray.put(section.toJson());
                for (ScreenAction action : actions) actionArray.put(action.toJson());
                for (String guard : guards) guardArray.put(guard);
                json.put("key", key);
                json.put("title", title);
                json.put("route", route);
                json.put("completionPercent", completionPercent());
                json.put("sections", sectionArray);
                json.put("actions", actionArray);
                json.put("guards", guardArray);
            } catch (Exception ignored) {
            }
            return json;
        }
    }

    public static final class ScreenSection {
        public final String name;
        public final String description;
        public final boolean done;

        public ScreenSection(String name, String description, boolean done) {
            this.name = name;
            this.description = description;
            this.done = done;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("name", name);
                json.put("description", description);
                json.put("done", done);
            } catch (Exception ignored) {
            }
            return json;
        }
    }

    public static final class ScreenAction {
        public final String name;
        public final String flow;
        public final String endpoint;

        public ScreenAction(String name, String flow, String endpoint) {
            this.name = name;
            this.flow = flow;
            this.endpoint = endpoint;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("name", name);
                json.put("flow", flow);
                json.put("endpoint", endpoint);
            } catch (Exception ignored) {
            }
            return json;
        }
    }

    public static Map<String, Integer> completionByScreen() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (ScreenBlueprint blueprint : all()) {
            map.put(blueprint.title, blueprint.completionPercent());
        }
        return map;
    }
}

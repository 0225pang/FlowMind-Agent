package com.flowmind.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central catalog for mobile feature parity with the web frontend.
 *
 * The first Android demo originally kept screen names and endpoints directly in MainActivity.
 * This catalog makes the mobile side easier to audit: every visible scene has a stable route,
 * endpoint list, permission hint, empty-state copy, and recommended recovery actions.
 */
public final class MobileFeatureCatalog {
    private MobileFeatureCatalog() {
    }

    public static final String ROUTE_AGENT = "/agent";
    public static final String ROUTE_KNOWLEDGE = "/knowledge";
    public static final String ROUTE_CONTENT = "/content";
    public static final String ROUTE_SCHOOLS = "/schools";
    public static final String ROUTE_SETTINGS = "/settings";

    public static final String PERMISSION_AGENT = "/api/agents/**";
    public static final String PERMISSION_KNOWLEDGE = "/api/knowledge/**";
    public static final String PERMISSION_CONTENT = "/api/content/**";
    public static final String PERMISSION_STUDENTS = "/api/students/**";
    public static final String PERMISSION_SCHOOLS = "/api/schools/**";
    public static final String PERMISSION_SCHOOL_PROJECTS = "/api/school-projects/**";
    public static final String PERMISSION_FEISHU = "/api/feishu/**";
    public static final String PERMISSION_SETTINGS = "/api/users/**";

    public static final class Endpoint {
        public final String name;
        public final String method;
        public final String path;
        public final String permission;
        public final String description;
        public final boolean streaming;
        public final boolean mutating;

        public Endpoint(String name, String method, String path, String permission,
                        String description, boolean streaming, boolean mutating) {
            this.name = name;
            this.method = method;
            this.path = path;
            this.permission = permission;
            this.description = description;
            this.streaming = streaming;
            this.mutating = mutating;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("name", name);
                json.put("method", method);
                json.put("path", path);
                json.put("permission", permission);
                json.put("description", description);
                json.put("streaming", streaming);
                json.put("mutating", mutating);
            } catch (Exception ignored) {
            }
            return json;
        }
    }

    public static final class Scene {
        public final String route;
        public final String title;
        public final String subtitle;
        public final String iconText;
        public final String permission;
        public final String emptyTitle;
        public final String emptyMessage;
        public final List<String> recoveryActions;
        public final List<Endpoint> endpoints;

        public Scene(String route, String title, String subtitle, String iconText,
                     String permission, String emptyTitle, String emptyMessage,
                     List<String> recoveryActions, List<Endpoint> endpoints) {
            this.route = route;
            this.title = title;
            this.subtitle = subtitle;
            this.iconText = iconText;
            this.permission = permission;
            this.emptyTitle = emptyTitle;
            this.emptyMessage = emptyMessage;
            this.recoveryActions = recoveryActions;
            this.endpoints = endpoints;
        }

        public boolean canOpen(List<String> permissions, boolean admin) {
            if (admin) return true;
            if (permissions == null) return false;
            return permissions.contains(permission) || permissions.contains("/**");
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            JSONArray endpointArray = new JSONArray();
            JSONArray actions = new JSONArray();
            try {
                for (Endpoint endpoint : endpoints) endpointArray.put(endpoint.toJson());
                for (String action : recoveryActions) actions.put(action);
                json.put("route", route);
                json.put("title", title);
                json.put("subtitle", subtitle);
                json.put("iconText", iconText);
                json.put("permission", permission);
                json.put("emptyTitle", emptyTitle);
                json.put("emptyMessage", emptyMessage);
                json.put("recoveryActions", actions);
                json.put("endpoints", endpointArray);
            } catch (Exception ignored) {
            }
            return json;
        }
    }

    public static List<Scene> scenes() {
        List<Scene> scenes = new ArrayList<>();
        scenes.add(agentScene());
        scenes.add(knowledgeScene());
        scenes.add(contentScene());
        scenes.add(schoolScene());
        scenes.add(settingsScene());
        return scenes;
    }

    public static Scene agentScene() {
        return new Scene(
                ROUTE_AGENT,
                "AI 工作台",
                "和 Web 端一致：总智能体自动选择知识库、飞书、内容或院校能力。",
                "AI",
                PERMISSION_AGENT,
                "还没有对话",
                "可以从快捷指令开始，也可以直接输入你的任务。",
                List.of("检查后端 Base URL", "确认 DeepSeek 或 MockLLM 可用", "优先尝试知识库检索类问题"),
                List.of(
                        new Endpoint("流式对话", "POST", "/api/agents/chat/stream", PERMISSION_AGENT, "SSE 流式回复、工具调用和 Thinking 展示", true, true),
                        new Endpoint("智能体清单", "GET", "/api/agents", PERMISSION_AGENT, "读取当前启用的 Agent 能力", false, false),
                        new Endpoint("新建会话", "POST", "/api/agents/sessions", PERMISSION_AGENT, "创建移动端会话", false, true),
                        new Endpoint("会话历史", "GET", "/api/agents/history/{agentType}/{sessionId}", PERMISSION_AGENT, "恢复历史消息", false, false)
                )
        );
    }

    public static Scene knowledgeScene() {
        return new Scene(
                ROUTE_KNOWLEDGE,
                "知识库",
                "和 Web 端一致：文档、标签、向量检索、飞书同步状态聚合展示。",
                "库",
                PERMISSION_KNOWLEDGE,
                "暂无知识文档",
                "知识库为空时，建议先在飞书同步页触发同步，再回到这里检索。",
                List.of("检查 Weaviate 是否启动", "检查飞书知识库 folderToken", "尝试使用更短的关键词"),
                List.of(
                        new Endpoint("知识库统计", "GET", "/api/knowledge/stats", PERMISSION_KNOWLEDGE, "读取文档数、标签数等指标", false, false),
                        new Endpoint("文档列表", "GET", "/api/knowledge/docs", PERMISSION_KNOWLEDGE, "移动端文档卡片数据", false, false),
                        new Endpoint("向量检索", "GET", "/api/knowledge/vector/search", PERMISSION_KNOWLEDGE, "按关键词检索 Weaviate 知识片段", false, false),
                        new Endpoint("知识库同步", "POST", "/api/knowledge/sync", PERMISSION_KNOWLEDGE, "触发飞书文档同步到本地知识库", false, true)
                )
        );
    }

    public static Scene contentScene() {
        return new Scene(
                ROUTE_CONTENT,
                "内容创作",
                "和 Web 端一致：主题库、文案库、日历、评分、图片引用和历史文案。",
                "创",
                PERMISSION_CONTENT,
                "暂无内容资产",
                "可以在 AI 工作台生成选题，或在内容页手动登记主题和文案。",
                List.of("检查当前角色是否有内容权限", "刷新主题库", "确认 MySQL 内容表已初始化"),
                List.of(
                        new Endpoint("主题库", "GET", "/api/content/themes", PERMISSION_CONTENT, "分页展示主题卡片", false, false),
                        new Endpoint("新增主题", "POST", "/api/content/themes", PERMISSION_CONTENT, "手动创建主题", false, true),
                        new Endpoint("主题评分", "PUT", "/api/content/themes/{id}/rating", PERMISSION_CONTENT, "移动端五星评分", false, true),
                        new Endpoint("历史文案", "GET", "/api/content/themes/{themeId}/drafts", PERMISSION_CONTENT, "查看主题下生成的历史文案", false, false),
                        new Endpoint("文案库", "GET", "/api/content/drafts", PERMISSION_CONTENT, "展示所有文案", false, false),
                        new Endpoint("编辑文案", "PUT", "/api/content/drafts/{draftId}", PERMISSION_CONTENT, "弹窗编辑文案标题和正文", false, true),
                        new Endpoint("文案图片", "POST", "/api/content/drafts/{draftId}/images", PERMISSION_CONTENT, "登记图片 URL 或配图建议", false, true),
                        new Endpoint("内容日历", "GET", "/api/content/calendar", PERMISSION_CONTENT, "按月展示发布标记", false, false)
                )
        );
    }

    public static Scene schoolScene() {
        return new Scene(
                ROUTE_SCHOOLS,
                "院校情报",
                "和 Web 端一致：学校列表、夏令营项目、截止趋势和 AI 匹配推荐。",
                "校",
                PERMISSION_SCHOOLS,
                "暂无院校项目",
                "院校项目为空时，可以先检查后端 Mock 数据或新增项目。",
                List.of("检查 school-service 是否启动", "查看 /api/school-projects", "尝试使用 AI 匹配推荐"),
                List.of(
                        new Endpoint("学校列表", "GET", "/api/schools", PERMISSION_SCHOOLS, "读取学校基础信息", false, false),
                        new Endpoint("项目列表", "GET", "/api/school-projects", PERMISSION_SCHOOL_PROJECTS, "读取夏令营和预推免项目", false, false),
                        new Endpoint("院校推荐", "POST", "/api/schools/recommend", PERMISSION_SCHOOLS, "基于学生画像推荐项目", false, true)
                )
        );
    }

    public static Scene settingsScene() {
        return new Scene(
                ROUTE_SETTINGS,
                "设置",
                "和 Web 端一致：后端地址、登录状态、权限状态和飞书状态。",
                "设",
                PERMISSION_SETTINGS,
                "暂无设置项",
                "设置页始终保留基础入口，便于用户修复后端地址或退出登录。",
                List.of("确认 Base URL 没有多余斜杠", "检查 token 是否过期", "无法访问时退出重新登录"),
                List.of(
                        new Endpoint("当前用户", "GET", "/api/users/me", PERMISSION_SETTINGS, "读取角色、权限和工作空间", false, false),
                        new Endpoint("飞书状态", "GET", "/api/feishu/sync/status", PERMISSION_FEISHU, "检查飞书同步能力", false, false),
                        new Endpoint("角色清单", "GET", "/api/roles", "/api/roles/**", "团队管理员查看角色权限", false, false),
                        new Endpoint("权限清单", "GET", "/api/permissions", "/api/permissions/**", "团队管理员查看权限点", false, false)
                )
        );
    }

    public static List<RoleProfile> roleProfiles() {
        List<RoleProfile> roles = new ArrayList<>();
        roles.add(new RoleProfile("TEAM_ADMIN", "团队管理员", "可查看和配置所有模块", true,
                List.of(PERMISSION_AGENT, PERMISSION_KNOWLEDGE, PERMISSION_CONTENT, PERMISSION_STUDENTS, PERMISSION_SCHOOLS, PERMISSION_SCHOOL_PROJECTS, PERMISSION_FEISHU, PERMISSION_SETTINGS)));
        roles.add(new RoleProfile("CONTENT_OPERATOR", "内容运营人员", "暂定可查看全部内容运营和知识资产", false,
                List.of(PERMISSION_AGENT, PERMISSION_KNOWLEDGE, PERMISSION_CONTENT, PERMISSION_STUDENTS, PERMISSION_SCHOOLS, PERMISSION_SCHOOL_PROJECTS, PERMISSION_FEISHU, PERMISSION_SETTINGS)));
        roles.add(new RoleProfile("EDU_CONSULTANT", "教育咨询老师", "暂定可查看全部咨询服务内容", false,
                List.of(PERMISSION_AGENT, PERMISSION_KNOWLEDGE, PERMISSION_CONTENT, PERMISSION_STUDENTS, PERMISSION_SCHOOLS, PERMISSION_SCHOOL_PROJECTS, PERMISSION_FEISHU, PERMISSION_SETTINGS)));
        roles.add(new RoleProfile("IP_OPERATOR", "个人IP运营者", "暂定可查看全部内容和运营数据", false,
                List.of(PERMISSION_AGENT, PERMISSION_KNOWLEDGE, PERMISSION_CONTENT, PERMISSION_STUDENTS, PERMISSION_SCHOOLS, PERMISSION_SCHOOL_PROJECTS, PERMISSION_FEISHU, PERMISSION_SETTINGS)));
        roles.add(new RoleProfile("STUDENT_USER", "学员用户", "仅可查看院校情报、知识库和 AI 工作台", false,
                List.of(PERMISSION_AGENT, PERMISSION_KNOWLEDGE, PERMISSION_SCHOOLS, PERMISSION_SCHOOL_PROJECTS, PERMISSION_SETTINGS)));
        return roles;
    }

    public static Map<String, String> endpointRecoveryMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("/api/auth/login", "检查账号是否在 MySQL sys_user 表中，密码当前为明文 123456。");
        map.put("/api/agents/chat/stream", "如果没有流式响应，先确认后端 SSE 未被代理缓存，再检查 DeepSeek API Key。");
        map.put("/api/knowledge/vector/search", "如果结果为空，先确认 Weaviate 有数据，再降低关键词复杂度。");
        map.put("/api/content/themes", "如果主题为空，先确认 content-service 初始化数据是否写入 MySQL。");
        map.put("/api/school-projects", "如果项目为空，检查 school-service Mock 数据或 MySQL 初始化。");
        map.put("/api/feishu/knowledge-base/files", "如果访问失败，检查 lark-cli 授权和 folderToken。");
        return map;
    }

    public static JSONObject toJsonSnapshot() {
        JSONObject json = new JSONObject();
        JSONArray sceneArray = new JSONArray();
        JSONArray roleArray = new JSONArray();
        try {
            for (Scene scene : scenes()) sceneArray.put(scene.toJson());
            for (RoleProfile role : roleProfiles()) roleArray.put(role.toJson());
            json.put("scenes", sceneArray);
            json.put("roles", roleArray);
            json.put("recovery", new JSONObject(endpointRecoveryMap()));
        } catch (Exception ignored) {
        }
        return json;
    }

    public static final class RoleProfile {
        public final String code;
        public final String name;
        public final String description;
        public final boolean protectedRole;
        public final List<String> permissions;

        public RoleProfile(String code, String name, String description, boolean protectedRole, List<String> permissions) {
            this.code = code;
            this.name = name;
            this.description = description;
            this.protectedRole = protectedRole;
            this.permissions = permissions;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            JSONArray permissionArray = new JSONArray();
            try {
                for (String permission : permissions) permissionArray.put(permission);
                json.put("code", code);
                json.put("name", name);
                json.put("description", description);
                json.put("protectedRole", protectedRole);
                json.put("permissions", permissionArray);
            } catch (Exception ignored) {
            }
            return json;
        }
    }
}

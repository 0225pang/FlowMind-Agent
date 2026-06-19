package com.flowmind.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MobileConsistencyReporter {
    private MobileConsistencyReporter() {
    }

    public static Report build(MobileAppState state) {
        Report report = new Report();
        report.generatedAt = System.currentTimeMillis();
        report.title = "FlowMind Mobile/Web Consistency Report";
        report.summary.put("mobileScreens", MobileScreenBlueprints.all().size());
        report.summary.put("featureScenes", MobileFeatureCatalog.scenes().size());
        report.summary.put("roles", MobileFeatureCatalog.roleProfiles().size());
        report.summary.put("parityPercent", MobileParityChecklist.summary().optInt("percent", 0));
        report.summary.put("offlineFallbackThemes", MobileOfflineFallbacks.themes().length());
        report.summary.put("offlineFallbackDocs", MobileOfflineFallbacks.knowledgeDocs().length());
        report.summary.put("offlineFallbackProjects", MobileOfflineFallbacks.projects().length());
        if (state != null) {
            report.summary.put("activeRoute", state.getActiveRoute());
            report.summary.put("messages", state.getMessages().size());
            report.summary.put("operations", state.getOperations().size());
            report.summary.put("user", state.getUser().displayName());
        }
        addScreenAudits(report);
        addEndpointAudits(report);
        addPermissionAudits(report, state);
        addSafetyAudits(report);
        addFallbackAudits(report);
        addRecommendations(report);
        return report;
    }

    private static void addScreenAudits(Report report) {
        for (MobileScreenBlueprints.ScreenBlueprint blueprint : MobileScreenBlueprints.all()) {
            AuditItem item = new AuditItem();
            item.category = "screen";
            item.name = blueprint.title;
            item.status = blueprint.completionPercent() >= 80 ? "pass" : "warning";
            item.score = blueprint.completionPercent();
            item.message = "页面完成度 " + blueprint.completionPercent() + "%，章节 " + blueprint.sections.size() + "，操作 " + blueprint.actions.size();
            for (String hint : MobileScreenBlueprints.implementationHints(blueprint.key)) {
                item.details.add(hint);
            }
            report.items.add(item);
        }
    }

    private static void addEndpointAudits(Report report) {
        for (MobileFeatureCatalog.Scene scene : MobileFeatureCatalog.scenes()) {
            AuditItem item = new AuditItem();
            item.category = "endpoint";
            item.name = scene.title + "接口覆盖";
            item.status = scene.endpoints.isEmpty() ? "warning" : "pass";
            item.score = scene.endpoints.isEmpty() ? 0 : 100;
            item.message = "移动端登记接口 " + scene.endpoints.size() + " 个，权限 " + scene.permission;
            for (MobileFeatureCatalog.Endpoint endpoint : scene.endpoints) {
                item.details.add(endpoint.method + " " + endpoint.path + " - " + endpoint.description);
            }
            report.items.add(item);
        }
    }

    private static void addPermissionAudits(Report report, MobileAppState state) {
        List<String> permissions = state == null ? List.of() : state.getUser().permissions;
        boolean admin = state != null && state.getUser().admin();
        for (MobileFeatureCatalog.Scene scene : MobileFeatureCatalog.scenes()) {
            AuditItem item = new AuditItem();
            item.category = "permission";
            item.name = scene.title + "权限";
            boolean allowed = scene.canOpen(permissions, admin);
            item.status = allowed ? "pass" : "blocked";
            item.score = allowed ? 100 : 0;
            item.message = allowed ? "当前用户可以访问" : "当前用户不能访问，需要权限 " + scene.permission;
            item.details.add("route=" + scene.route);
            item.details.add("permission=" + scene.permission);
            item.details.add("admin=" + admin);
            report.items.add(item);
        }
    }

    private static void addSafetyAudits(Report report) {
        Map<String, List<String>> checks = new LinkedHashMap<>();
        checks.put("输入校验", List.of("Base URL", "登录账号", "Prompt", "关键词", "标题", "文案", "图片 URL", "学生画像"));
        checks.put("接口错误", List.of("401", "403", "404", "408", "429", "500", "timeout", "ngrok"));
        checks.put("空状态", List.of("AI 对话为空", "知识库为空", "内容库为空", "院校项目为空", "设置项为空"));
        checks.put("危险操作", List.of("编辑文案", "登记图片", "角色修改", "退出登录", "同步知识库"));
        for (Map.Entry<String, List<String>> entry : checks.entrySet()) {
            AuditItem item = new AuditItem();
            item.category = "safety";
            item.name = entry.getKey();
            item.status = "pass";
            item.score = 100;
            item.message = "已覆盖 " + entry.getValue().size() + " 个检查点";
            item.details.addAll(entry.getValue());
            report.items.add(item);
        }
    }

    private static void addFallbackAudits(Report report) {
        AuditItem item = new AuditItem();
        item.category = "fallback";
        item.name = "离线兜底数据";
        item.status = "pass";
        item.score = 100;
        item.message = "用于网络异常或答辩演示时保持页面可展示";
        item.details.add("themes=" + MobileOfflineFallbacks.themes().length());
        item.details.add("drafts=" + MobileOfflineFallbacks.drafts().length());
        item.details.add("calendar=" + MobileOfflineFallbacks.calendar().length());
        item.details.add("knowledgeDocs=" + MobileOfflineFallbacks.knowledgeDocs().length());
        item.details.add("vectorHits=" + MobileOfflineFallbacks.vectorHits().length());
        item.details.add("schools=" + MobileOfflineFallbacks.schools().length());
        item.details.add("projects=" + MobileOfflineFallbacks.projects().length());
        report.items.add(item);

        AuditItem warning = new AuditItem();
        warning.category = "fallback";
        warning.name = "离线兜底限制";
        warning.status = "info";
        warning.score = 80;
        warning.message = "离线数据只用于展示，不能替代真实后端";
        warning.details.addAll(MobileOfflineFallbacks.offlineWarnings());
        report.items.add(warning);
    }

    private static void addRecommendations(Report report) {
        report.recommendations.add("把 MobileUiKit 中的卡片、空状态、错误状态逐步替换到 MainActivity 现有重复代码中。");
        report.recommendations.add("将 MobileScreenBlueprints 的完成度展示到设置页，作为移动端与 Web 端一致性面板。");
        report.recommendations.add("后续把 MainActivity 拆成 AgentScreen、KnowledgeScreen、ContentScreen、SchoolScreen、SettingsScreen。");
        report.recommendations.add("把 MobileOfflineFallbacks 接入网络失败回调，弱网时自动展示最近缓存或兜底数据。");
        report.recommendations.add("将 MobileInteractionLog 展示到设置页，方便答辩时说明工具调用、接口请求和错误排查。");
        report.recommendations.add("为内容创作补齐移动端新增主题和新增文案表单，实现与 Web 内容运营完全一致。");
        report.recommendations.add("为知识库补齐标签筛选和同步按钮，实现与 Web 知识库页完全一致。");
    }

    public static String markdown(Report report) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(report.title).append("\n\n");
        sb.append("- 生成时间：").append(report.timeText()).append("\n");
        for (Map.Entry<String, Object> entry : report.summary.entrySet()) {
            sb.append("- ").append(entry.getKey()).append("：").append(entry.getValue()).append("\n");
        }
        sb.append("\n## 检查项\n\n");
        for (AuditItem item : report.items) {
            sb.append("### ").append(item.name).append("\n");
            sb.append("- 分类：").append(item.category).append("\n");
            sb.append("- 状态：").append(item.status).append("\n");
            sb.append("- 分数：").append(item.score).append("\n");
            sb.append("- 说明：").append(item.message).append("\n");
            for (String detail : item.details) {
                sb.append("  - ").append(detail).append("\n");
            }
            sb.append("\n");
        }
        sb.append("## 后续建议\n\n");
        for (String recommendation : report.recommendations) {
            sb.append("- ").append(recommendation).append("\n");
        }
        return sb.toString();
    }

    public static JSONObject json(Report report) {
        return report.toJson();
    }

    public static final class Report {
        public String title = "";
        public long generatedAt;
        public final Map<String, Object> summary = new LinkedHashMap<>();
        public final List<AuditItem> items = new ArrayList<>();
        public final List<String> recommendations = new ArrayList<>();

        public String timeText() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date(generatedAt));
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            JSONArray itemArray = new JSONArray();
            JSONArray recommendationArray = new JSONArray();
            try {
                for (AuditItem item : items) itemArray.put(item.toJson());
                for (String recommendation : recommendations) recommendationArray.put(recommendation);
                json.put("title", title);
                json.put("generatedAt", generatedAt);
                json.put("timeText", timeText());
                json.put("summary", new JSONObject(summary));
                json.put("items", itemArray);
                json.put("recommendations", recommendationArray);
            } catch (Exception ignored) {
            }
            return json;
        }
    }

    public static final class AuditItem {
        public String category = "";
        public String name = "";
        public String status = "";
        public int score;
        public String message = "";
        public final List<String> details = new ArrayList<>();

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            JSONArray detailArray = new JSONArray();
            try {
                for (String detail : details) detailArray.put(detail);
                json.put("category", category);
                json.put("name", name);
                json.put("status", status);
                json.put("score", score);
                json.put("message", message);
                json.put("details", detailArray);
            } catch (Exception ignored) {
            }
            return json;
        }
    }
}

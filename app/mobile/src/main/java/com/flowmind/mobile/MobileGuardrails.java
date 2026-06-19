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

public final class MobileGuardrails {
    private MobileGuardrails() {
    }

    public static final int MAX_PROMPT_LENGTH = 4000;
    public static final int MAX_TITLE_LENGTH = 120;
    public static final int MAX_COPY_LENGTH = 12000;
    public static final int MIN_PASSWORD_LENGTH = 3;

    public static GuardResult validateBaseUrl(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) {
            return GuardResult.block("后端地址不能为空", "请在设置页填写后端 Base URL，例如 https://xxx.ngrok-free.dev");
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return GuardResult.block("后端地址必须以 http:// 或 https:// 开头", "请检查是否漏写协议前缀。");
        }
        if (value.contains(" ")) {
            return GuardResult.block("后端地址不能包含空格", "请删除复制时带入的空格或换行。");
        }
        if (value.endsWith("/")) {
            return GuardResult.warn("后端地址末尾包含 /，系统会自动去掉", normalizeBaseUrl(value));
        }
        return GuardResult.ok(normalizeBaseUrl(value));
    }

    public static String normalizeBaseUrl(String input) {
        String value = input == null || input.trim().isEmpty() ? BuildConfig.DEFAULT_BASE_URL : input.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public static GuardResult validateLogin(String username, String password) {
        String u = trim(username);
        String p = trim(password);
        if (u.isEmpty()) return GuardResult.block("请输入账号", "可以使用 admin、content、teacher、ip 或 student。");
        if (p.isEmpty()) return GuardResult.block("请输入密码", "Demo 阶段默认密码是 123456。");
        if (p.length() < MIN_PASSWORD_LENGTH) return GuardResult.block("密码长度过短", "请确认是否输入完整。");
        if (u.length() > 64) return GuardResult.block("账号过长", "账号长度不能超过 64 个字符。");
        return GuardResult.ok("login-ready");
    }

    public static GuardResult validatePrompt(String prompt) {
        String value = trim(prompt);
        if (value.isEmpty()) return GuardResult.block("请输入任务内容", "可以点击快捷指令，或输入要生成/检索/同步的内容。");
        if (value.length() > MAX_PROMPT_LENGTH) {
            return GuardResult.block("输入内容太长", "请拆成多轮对话，当前限制 " + MAX_PROMPT_LENGTH + " 字。");
        }
        if (looksLikeSecret(value)) {
            return GuardResult.warn("输入中疑似包含密钥", "请确认不要把 API Key、密码或 Token 发给模型。");
        }
        return GuardResult.ok(value);
    }

    public static GuardResult validateSearchKeyword(String keyword) {
        String value = trim(keyword);
        if (value.isEmpty()) return GuardResult.block("请输入检索关键词", "建议使用 2-20 个字，例如：课程论文、保研简历、夏令营材料。");
        if (value.length() < 2) return GuardResult.warn("关键词较短，结果可能不稳定", value);
        if (value.length() > 80) return GuardResult.warn("关键词较长，建议缩短后再检索", value.substring(0, 80));
        return GuardResult.ok(value);
    }

    public static GuardResult validateTitle(String title) {
        String value = trim(title);
        if (value.isEmpty()) return GuardResult.block("标题不能为空", "请填写主题或文案标题。");
        if (value.length() > MAX_TITLE_LENGTH) return GuardResult.block("标题太长", "请控制在 " + MAX_TITLE_LENGTH + " 字以内。");
        return GuardResult.ok(value);
    }

    public static GuardResult validateCopyText(String text) {
        String value = trim(text);
        if (value.isEmpty()) return GuardResult.block("文案内容不能为空", "请填写正文，或先使用 AI 工作台生成。");
        if (value.length() > MAX_COPY_LENGTH) return GuardResult.block("文案内容太长", "请拆分保存，当前限制 " + MAX_COPY_LENGTH + " 字。");
        return GuardResult.ok(value);
    }

    public static GuardResult validateRating(int rating) {
        if (rating < 1 || rating > 5) return GuardResult.block("评分必须是 1-5 星", "请点击星星进行评分。");
        return GuardResult.ok(String.valueOf(rating));
    }

    public static GuardResult validateImageUrl(String imageUrl) {
        String value = trim(imageUrl);
        if (value.isEmpty()) return GuardResult.block("图片 URL 不能为空", "如果没有图片，可以先在文案里保留配图建议。");
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return GuardResult.block("图片 URL 格式不正确", "请填写 http 或 https 开头的图片地址。");
        }
        if (value.length() > 1000) return GuardResult.block("图片 URL 过长", "请检查是否复制了错误内容。");
        return GuardResult.ok(value);
    }

    public static GuardResult validateStudentProfile(String profile) {
        String value = trim(profile);
        if (value.isEmpty()) return GuardResult.block("请输入学生画像", "至少包含 GPA、排名、英语成绩、科研经历或目标方向。");
        List<String> missing = new ArrayList<>();
        if (!containsAny(value, "GPA", "绩点", "成绩")) missing.add("GPA/成绩");
        if (!containsAny(value, "排名", "前", "%")) missing.add("排名");
        if (!containsAny(value, "六级", "雅思", "托福", "英语")) missing.add("英语");
        if (missing.isEmpty()) return GuardResult.ok(value);
        return GuardResult.warn("画像信息不完整", "建议补充：" + String.join("、", missing));
    }

    public static GuardResult validatePermission(String route, List<String> permissions, boolean admin) {
        if (admin) return GuardResult.ok(route);
        MobileFeatureCatalog.Scene scene = findScene(route);
        if (scene == null) return GuardResult.block("未知页面", "当前移动端没有注册这个页面。");
        if (scene.canOpen(permissions, false)) return GuardResult.ok(route);
        return GuardResult.block("暂无访问权限", "当前角色不能访问「" + scene.title + "」，请联系团队管理员调整角色权限。");
    }

    public static MobileFeatureCatalog.Scene findScene(String route) {
        for (MobileFeatureCatalog.Scene scene : MobileFeatureCatalog.scenes()) {
            if (scene.route.equals(route)) return scene;
        }
        return null;
    }

    public static String friendlyHttpError(String message) {
        String raw = message == null ? "" : message;
        if (raw.contains("401")) return "登录状态失效，请退出后重新登录。";
        if (raw.contains("403")) return "当前账号无权限访问该接口。";
        if (raw.contains("404")) return "接口不存在，请检查后端版本或 Base URL。";
        if (raw.contains("500")) return "后端处理异常，请查看控制台日志。";
        if (raw.toLowerCase(Locale.ROOT).contains("timeout")) return "请求超时，请检查网络、ngrok 或后端服务。";
        if (raw.toLowerCase(Locale.ROOT).contains("failed to connect")) return "无法连接后端，请检查 Base URL。";
        if (raw.toLowerCase(Locale.ROOT).contains("ngrok")) return "ngrok 连接异常，请确认穿透地址仍然有效。";
        return raw.isEmpty() ? "请求失败，请稍后重试。" : raw;
    }

    public static String friendlyEmptyMessage(String route) {
        MobileFeatureCatalog.Scene scene = findScene(route);
        if (scene == null) return "暂无数据。";
        return scene.emptyTitle + "：" + scene.emptyMessage;
    }

    public static List<String> recoveryActions(String route) {
        MobileFeatureCatalog.Scene scene = findScene(route);
        if (scene == null) return List.of("刷新页面", "检查后端服务");
        return scene.recoveryActions;
    }

    public static String normalizeMarkdown(String value) {
        String text = value == null ? "" : value.trim();
        text = text.replace("\r\n", "\n");
        text = text.replace("\r", "\n");
        while (text.contains("\n\n\n")) text = text.replace("\n\n\n", "\n\n");
        return text;
    }

    public static String compactOneLine(String value, int max) {
        String text = value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
        while (text.contains("  ")) text = text.replace("  ", " ");
        if (text.length() <= max) return text;
        return text.substring(0, Math.max(0, max - 3)) + "...";
    }

    public static String safeJsonString(JSONObject object, String key, String fallback) {
        if (object == null) return fallback;
        String value = object.optString(key, "");
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value)) return fallback;
        return value;
    }

    public static int safeJsonInt(JSONObject object, String key, int fallback) {
        if (object == null || !object.has(key)) return fallback;
        try {
            return object.optInt(key, fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static long safeJsonLong(JSONObject object, String key, long fallback) {
        if (object == null || !object.has(key)) return fallback;
        try {
            return object.optLong(key, fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static JSONArray safeArray(Object data) {
        if (data instanceof JSONArray) return (JSONArray) data;
        if (data instanceof JSONObject) {
            JSONObject object = (JSONObject) data;
            JSONArray records = object.optJSONArray("records");
            if (records != null) return records;
            JSONArray items = object.optJSONArray("items");
            if (items != null) return items;
            JSONArray list = object.optJSONArray("list");
            if (list != null) return list;
            JSONArray nested = object.optJSONArray("data");
            if (nested != null) return nested;
        }
        return new JSONArray();
    }

    public static JSONObject buildErrorPayload(String scene, String operation, String message) {
        JSONObject json = new JSONObject();
        try {
            json.put("scene", scene);
            json.put("operation", operation);
            json.put("message", friendlyHttpError(message));
            json.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date()));
            JSONArray actions = new JSONArray();
            for (String action : recoveryActions(scene)) actions.put(action);
            json.put("recoveryActions", actions);
        } catch (Exception ignored) {
        }
        return json;
    }

    public static JSONObject buildOperationPayload(String operation, String status, Map<String, Object> values) {
        JSONObject json = new JSONObject();
        try {
            json.put("operation", operation);
            json.put("status", status);
            json.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date()));
            if (values != null) {
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    json.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception ignored) {
        }
        return json;
    }

    public static Map<String, Object> mapOf(String k1, Object v1) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(k1, v1);
        return map;
    }

    public static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> map = mapOf(k1, v1);
        map.put(k2, v2);
        return map;
    }

    public static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        Map<String, Object> map = mapOf(k1, v1, k2, v2);
        map.put(k3, v3);
        return map;
    }

    public static boolean looksLikeSecret(String value) {
        if (value == null) return false;
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("sk-")
                || lower.contains("api_key")
                || lower.contains("apikey")
                || lower.contains("password")
                || lower.contains("secret")
                || lower.contains("token=");
    }

    private static boolean containsAny(String value, String... candidates) {
        if (value == null) return false;
        for (String candidate : candidates) {
            if (value.contains(candidate)) return true;
        }
        return false;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public enum GuardLevel {
        OK,
        WARN,
        BLOCK
    }

    public static final class GuardResult {
        public final GuardLevel level;
        public final String title;
        public final String message;
        public final String normalizedValue;

        private GuardResult(GuardLevel level, String title, String message, String normalizedValue) {
            this.level = level;
            this.title = title;
            this.message = message;
            this.normalizedValue = normalizedValue;
        }

        public static GuardResult ok(String normalizedValue) {
            return new GuardResult(GuardLevel.OK, "ok", "", normalizedValue);
        }

        public static GuardResult warn(String title, String message) {
            return new GuardResult(GuardLevel.WARN, title, message, message);
        }

        public static GuardResult block(String title, String message) {
            return new GuardResult(GuardLevel.BLOCK, title, message, "");
        }

        public boolean ok() {
            return level == GuardLevel.OK || level == GuardLevel.WARN;
        }

        public boolean blocked() {
            return level == GuardLevel.BLOCK;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("level", level.name());
                json.put("title", title);
                json.put("message", message);
                json.put("normalizedValue", normalizedValue);
            } catch (Exception ignored) {
            }
            return json;
        }
    }
}

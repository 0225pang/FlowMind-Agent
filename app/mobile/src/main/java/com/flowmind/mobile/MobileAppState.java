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

public final class MobileAppState {
    private String baseUrl = BuildConfig.DEFAULT_BASE_URL;
    private String token = "mock-jwt.demo";
    private String activeRoute = MobileFeatureCatalog.ROUTE_AGENT;
    private String sessionId = "";
    private MobileModels.UserProfile user = new MobileModels.UserProfile();
    private final List<MobileModels.ChatMessage> messages = new ArrayList<>();
    private final List<MobileModels.OperationState> operations = new ArrayList<>();
    private final Map<String, JSONObject> cache = new LinkedHashMap<>();
    private long lastHydratedAt = 0;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = MobileGuardrails.normalizeBaseUrl(baseUrl);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        if (token != null && !token.isBlank()) this.token = token;
    }

    public String getActiveRoute() {
        return activeRoute;
    }

    public MobileGuardrails.GuardResult switchRoute(String route) {
        MobileGuardrails.GuardResult result = MobileGuardrails.validatePermission(route, user.permissions, user.admin());
        if (result.ok()) activeRoute = route;
        return result;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) this.sessionId = sessionId;
    }

    public MobileModels.UserProfile getUser() {
        return user;
    }

    public void setUser(MobileModels.UserProfile user) {
        this.user = user == null ? new MobileModels.UserProfile() : user;
        lastHydratedAt = System.currentTimeMillis();
    }

    public List<MobileModels.ChatMessage> getMessages() {
        return messages;
    }

    public void addUserMessage(String content) {
        MobileModels.ChatMessage message = new MobileModels.ChatMessage();
        message.id = "u-" + System.currentTimeMillis();
        message.role = "user";
        message.content = content;
        messages.add(message);
    }

    public MobileModels.ChatMessage addAssistantMessage() {
        MobileModels.ChatMessage message = new MobileModels.ChatMessage();
        message.id = "a-" + System.currentTimeMillis();
        message.role = "assistant";
        message.streaming = true;
        messages.add(message);
        return message;
    }

    public void appendAssistantDelta(MobileModels.ChatMessage message, String delta) {
        if (message == null || delta == null) return;
        message.content = message.content + delta;
    }

    public void appendAssistantThinking(MobileModels.ChatMessage message, String delta) {
        if (message == null || delta == null) return;
        message.thinking = message.thinking + delta;
    }

    public void appendAssistantTrace(MobileModels.ChatMessage message, String trace) {
        if (message == null || trace == null || trace.isBlank()) return;
        if (!message.trace.isBlank()) message.trace += "\n";
        message.trace += trace;
    }

    public void finishAssistantMessage(MobileModels.ChatMessage message, boolean failed) {
        if (message == null) return;
        message.streaming = false;
        message.failed = failed;
    }

    public MobileModels.OperationState beginOperation(String scene, String operation) {
        MobileModels.OperationState state = new MobileModels.OperationState();
        state.scene = scene;
        state.operation = operation;
        state.loading = true;
        state.startedAt = System.currentTimeMillis();
        operations.add(0, state);
        trimOperations();
        return state;
    }

    public void finishOperation(MobileModels.OperationState state, String message) {
        if (state == null) return;
        state.loading = false;
        state.failed = false;
        state.message = message;
        state.endedAt = System.currentTimeMillis();
    }

    public void failOperation(MobileModels.OperationState state, String error) {
        if (state == null) return;
        state.loading = false;
        state.failed = true;
        state.message = MobileGuardrails.friendlyHttpError(error);
        state.endedAt = System.currentTimeMillis();
    }

    public List<MobileModels.OperationState> getOperations() {
        return operations;
    }

    public void putCache(String key, JSONObject value) {
        if (key == null || key.isBlank() || value == null) return;
        cache.put(key, value);
        trimCache();
    }

    public JSONObject getCache(String key) {
        return cache.get(key);
    }

    public boolean hasFreshUser(long maxAgeMs) {
        return user != null && user.username != null && !user.username.isBlank()
                && System.currentTimeMillis() - lastHydratedAt < maxAgeMs;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        JSONArray messageArray = new JSONArray();
        JSONArray operationArray = new JSONArray();
        JSONArray cacheKeys = new JSONArray();
        try {
            for (MobileModels.ChatMessage message : messages) messageArray.put(message.toJson());
            for (MobileModels.OperationState state : operations) {
                JSONObject item = new JSONObject();
                item.put("scene", state.scene);
                item.put("operation", state.operation);
                item.put("loading", state.loading);
                item.put("failed", state.failed);
                item.put("message", state.message);
                item.put("durationMs", state.durationMs());
                operationArray.put(item);
            }
            for (String key : cache.keySet()) cacheKeys.put(key);
            json.put("baseUrl", baseUrl);
            json.put("activeRoute", activeRoute);
            json.put("sessionId", sessionId);
            json.put("user", user == null ? new JSONObject() : user.toJson());
            json.put("messages", messageArray);
            json.put("operations", operationArray);
            json.put("cacheKeys", cacheKeys);
            json.put("lastHydratedAt", lastHydratedAt);
        } catch (Exception ignored) {
        }
        return json;
    }

    public static MobileAppState fromJson(JSONObject json) {
        MobileAppState state = new MobileAppState();
        if (json == null) return state;
        state.setBaseUrl(json.optString("baseUrl", BuildConfig.DEFAULT_BASE_URL));
        state.setToken(json.optString("token", "mock-jwt.demo"));
        state.activeRoute = json.optString("activeRoute", MobileFeatureCatalog.ROUTE_AGENT);
        state.sessionId = json.optString("sessionId", "");
        state.user = MobileDataMappers.user(json.optJSONObject("user"));
        JSONArray messages = json.optJSONArray("messages");
        if (messages != null) {
            for (int i = 0; i < messages.length(); i++) {
                JSONObject item = messages.optJSONObject(i);
                if (item == null) continue;
                MobileModels.ChatMessage message = new MobileModels.ChatMessage();
                message.id = item.optString("id");
                message.role = item.optString("role");
                message.content = item.optString("content");
                message.thinking = item.optString("thinking");
                message.trace = item.optString("trace");
                message.streaming = item.optBoolean("streaming");
                message.failed = item.optBoolean("failed");
                state.messages.add(message);
            }
        }
        return state;
    }

    public List<String> diagnostics() {
        List<String> list = new ArrayList<>();
        list.add("后端地址：" + baseUrl);
        list.add("当前页面：" + activeRoute);
        list.add("当前用户：" + (user == null ? "未加载" : user.displayName()));
        list.add("角色：" + (user == null ? "未加载" : String.join(",", user.roles)));
        list.add("权限数：" + (user == null ? 0 : user.permissions.size()));
        list.add("会话 ID：" + (sessionId == null || sessionId.isBlank() ? "尚未创建" : sessionId));
        list.add("消息数：" + messages.size());
        list.add("最近操作：" + operations.size());
        list.add("缓存项：" + cache.size());
        list.add("状态生成时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date()));
        return list;
    }

    public boolean shouldWarnBeforeLogout() {
        for (MobileModels.ChatMessage message : messages) {
            if (message.streaming) return true;
        }
        for (MobileModels.OperationState operation : operations) {
            if (operation.loading) return true;
        }
        return false;
    }

    public boolean canUseScene(String route) {
        return MobileGuardrails.validatePermission(route, user.permissions, user.admin()).ok();
    }

    public String firstAvailableRoute() {
        for (MobileFeatureCatalog.Scene scene : MobileFeatureCatalog.scenes()) {
            if (scene.canOpen(user.permissions, user.admin())) return scene.route;
        }
        return MobileFeatureCatalog.ROUTE_SETTINGS;
    }

    public void clearConversation() {
        messages.clear();
        sessionId = "";
    }

    public void clearCache() {
        cache.clear();
    }

    private void trimOperations() {
        while (operations.size() > 50) operations.remove(operations.size() - 1);
    }

    private void trimCache() {
        while (cache.size() > 80) {
            String first = cache.keySet().iterator().next();
            cache.remove(first);
        }
    }
}

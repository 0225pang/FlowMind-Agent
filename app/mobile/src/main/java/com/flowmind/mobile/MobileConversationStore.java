package com.flowmind.mobile;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Local conversation persistence for the Android AI workspace.
 *
 * Backend sessions are still the source of truth when the API is reachable, but
 * mobile users expect the most recent chat to survive navigation, process death
 * and app restart. This store keeps a compact transcript in SharedPreferences.
 * It is deliberately isolated from MainActivity so a future teammate can switch
 * the implementation to SQLite, Room or backend-backed sync without touching the
 * chat UI.
 */
public final class MobileConversationStore {
    private static final String KEY_ACTIVE_SESSION = "mobile.active.conversation.session";
    private static final String KEY_SESSION_INDEX = "mobile.conversation.session.index";
    private static final String KEY_SESSION_PREFIX = "mobile.conversation.session.";
    private static final int MAX_SESSIONS = 30;
    private static final int MAX_MESSAGES_PER_SESSION = 80;
    private static final int MAX_MESSAGE_CHARS = 24_000;

    private final SharedPreferences prefs;

    public MobileConversationStore(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public ConversationSnapshot loadActiveOrLatest() {
        String activeId = prefs.getString(KEY_ACTIVE_SESSION, "");
        ConversationSnapshot active = load(activeId);
        if (active != null) {
            return active;
        }
        List<ConversationSummary> summaries = listSummaries();
        if (!summaries.isEmpty()) {
            ConversationSnapshot latest = load(summaries.get(0).sessionId);
            if (latest != null) {
                setActive(latest.sessionId);
                return latest;
            }
        }
        return null;
    }

    public ConversationSnapshot startNew(String seedTitle) {
        long now = System.currentTimeMillis();
        String localId = "local-" + now + "-" + UUID.randomUUID().toString().substring(0, 8);
        ConversationSnapshot snapshot = new ConversationSnapshot(
                localId,
                safeTitle(seedTitle, "New conversation"),
                now,
                now,
                new JSONArray(),
                new JSONArray()
        );
        save(snapshot);
        setActive(localId);
        return snapshot;
    }

    public ConversationSnapshot ensureActive() {
        ConversationSnapshot snapshot = loadActiveOrLatest();
        if (snapshot == null) {
            snapshot = startNew("FlowMind conversation");
        }
        return snapshot;
    }

    public ConversationSnapshot load(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return null;
        }
        String raw = prefs.getString(KEY_SESSION_PREFIX + sessionId, "");
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(raw);
            return ConversationSnapshot.fromJson(json);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void setActive(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            prefs.edit().remove(KEY_ACTIVE_SESSION).apply();
            return;
        }
        prefs.edit().putString(KEY_ACTIVE_SESSION, sessionId).apply();
    }

    public void mapBackendSession(String localSessionId, String backendSessionId) {
        if (localSessionId == null || backendSessionId == null) {
            return;
        }
        String local = localSessionId.trim();
        String backend = backendSessionId.trim();
        if (local.isEmpty() || backend.isEmpty() || local.equals(backend)) {
            return;
        }
        ConversationSnapshot snapshot = load(local);
        if (snapshot == null) {
            return;
        }
        ConversationSnapshot mapped = snapshot.withSessionId(backend);
        save(mapped);
        remove(local);
        setActive(backend);
    }

    public ConversationSnapshot appendUserMessage(String sessionId, String content) {
        ConversationSnapshot snapshot = load(sessionId);
        if (snapshot == null) {
            snapshot = startNew(content);
        }
        ConversationMessage message = ConversationMessage.user(content);
        ConversationSnapshot updated = snapshot.append(message).touch().withTitleIfDefault(content);
        save(updated);
        setActive(updated.sessionId);
        return updated;
    }

    public ConversationSnapshot appendAssistantMessage(String sessionId, String content, JSONArray tools, String reasoning, String status) {
        ConversationSnapshot snapshot = load(sessionId);
        if (snapshot == null) {
            snapshot = ensureActive();
        }
        ConversationMessage message = ConversationMessage.assistant(content, tools, reasoning, status);
        ConversationSnapshot updated = snapshot.append(message).touch();
        save(updated);
        setActive(updated.sessionId);
        return updated;
    }

    public ConversationSnapshot appendSystemMessage(String sessionId, String content) {
        ConversationSnapshot snapshot = load(sessionId);
        if (snapshot == null) {
            snapshot = ensureActive();
        }
        ConversationSnapshot updated = snapshot.append(ConversationMessage.system(content)).touch();
        save(updated);
        setActive(updated.sessionId);
        return updated;
    }

    public List<ConversationSummary> listSummaries() {
        List<String> ids = readIndex();
        List<ConversationSummary> summaries = new ArrayList<>();
        for (String id : ids) {
            ConversationSnapshot snapshot = load(id);
            if (snapshot != null) {
                summaries.add(snapshot.toSummary());
            }
        }
        summaries.sort((a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        return summaries;
    }

    public void remove(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }
        String id = sessionId.trim();
        List<String> ids = readIndex();
        ids.remove(id);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_SESSION_PREFIX + id);
        editor.putString(KEY_SESSION_INDEX, new JSONArray(ids).toString());
        if (id.equals(prefs.getString(KEY_ACTIVE_SESSION, ""))) {
            editor.remove(KEY_ACTIVE_SESSION);
        }
        editor.apply();
    }

    public void clearAll() {
        SharedPreferences.Editor editor = prefs.edit();
        for (String id : readIndex()) {
            editor.remove(KEY_SESSION_PREFIX + id);
        }
        editor.remove(KEY_SESSION_INDEX);
        editor.remove(KEY_ACTIVE_SESSION);
        editor.apply();
    }

    private void save(ConversationSnapshot snapshot) {
        if (snapshot == null || snapshot.sessionId.trim().isEmpty()) {
            return;
        }
        ConversationSnapshot trimmed = snapshot.trimmed();
        List<String> ids = readIndex();
        ids.remove(trimmed.sessionId);
        ids.add(0, trimmed.sessionId);
        while (ids.size() > MAX_SESSIONS) {
            String removed = ids.remove(ids.size() - 1);
            prefs.edit().remove(KEY_SESSION_PREFIX + removed).apply();
        }
        prefs.edit()
                .putString(KEY_SESSION_PREFIX + trimmed.sessionId, trimmed.toJson().toString())
                .putString(KEY_SESSION_INDEX, new JSONArray(ids).toString())
                .apply();
    }

    private List<String> readIndex() {
        List<String> ids = new ArrayList<>();
        String raw = prefs.getString(KEY_SESSION_INDEX, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                String id = array.optString(i, "").trim();
                if (!id.isEmpty() && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        } catch (Exception ignored) {
        }
        return ids;
    }

    private static String safeTitle(String raw, String fallback) {
        String value = raw == null ? "" : raw.replace('\n', ' ').trim();
        if (value.isEmpty()) {
            value = fallback;
        }
        if (value.length() > 40) {
            value = value.substring(0, 40) + "...";
        }
        return value;
    }

    private static String safeContent(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() > MAX_MESSAGE_CHARS) {
            return value.substring(0, MAX_MESSAGE_CHARS) + "\n\n[Message clipped locally. Full content may still exist on backend.]";
        }
        return value;
    }

    private static JSONArray safeArray(JSONArray raw) {
        return raw == null ? new JSONArray() : raw;
    }

    private static String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }
        return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    public static final class ConversationSnapshot {
        public final String sessionId;
        public final String title;
        public final long createdAt;
        public final long updatedAt;
        public final JSONArray messages;
        public final JSONArray tags;

        public ConversationSnapshot(String sessionId, String title, long createdAt, long updatedAt, JSONArray messages, JSONArray tags) {
            this.sessionId = sessionId == null ? "" : sessionId;
            this.title = safeTitle(title, "FlowMind conversation");
            this.createdAt = createdAt <= 0 ? System.currentTimeMillis() : createdAt;
            this.updatedAt = updatedAt <= 0 ? this.createdAt : updatedAt;
            this.messages = safeArray(messages);
            this.tags = safeArray(tags);
        }

        public ConversationSnapshot withSessionId(String nextSessionId) {
            return new ConversationSnapshot(nextSessionId, title, createdAt, updatedAt, messages, tags);
        }

        public ConversationSnapshot withTitleIfDefault(String prompt) {
            if (!"FlowMind conversation".equals(title) && !"New conversation".equals(title)) {
                return this;
            }
            return new ConversationSnapshot(sessionId, safeTitle(prompt, title), createdAt, updatedAt, messages, tags);
        }

        public ConversationSnapshot append(ConversationMessage message) {
            JSONArray next = new JSONArray();
            int start = Math.max(0, messages.length() - MAX_MESSAGES_PER_SESSION + 1);
            for (int i = start; i < messages.length(); i++) {
                next.put(messages.opt(i));
            }
            next.put(message.toJson());
            return new ConversationSnapshot(sessionId, title, createdAt, updatedAt, next, tags);
        }

        public ConversationSnapshot touch() {
            return new ConversationSnapshot(sessionId, title, createdAt, System.currentTimeMillis(), messages, tags);
        }

        public ConversationSnapshot trimmed() {
            JSONArray next = new JSONArray();
            int start = Math.max(0, messages.length() - MAX_MESSAGES_PER_SESSION);
            for (int i = start; i < messages.length(); i++) {
                JSONObject item = messages.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                JSONObject copy = new JSONObject();
                try {
                    copy.put("role", item.optString("role"));
                    copy.put("content", safeContent(item.optString("content")));
                    copy.put("createdAt", item.optLong("createdAt", System.currentTimeMillis()));
                    copy.put("status", item.optString("status"));
                    copy.put("reasoning", safeContent(item.optString("reasoning")));
                    copy.put("tools", item.optJSONArray("tools") == null ? new JSONArray() : item.optJSONArray("tools"));
                } catch (Exception ignored) {
                }
                next.put(copy);
            }
            return new ConversationSnapshot(sessionId, title, createdAt, updatedAt, next, tags);
        }

        public ConversationSummary toSummary() {
            String preview = "";
            String status = "";
            for (int i = messages.length() - 1; i >= 0; i--) {
                JSONObject item = messages.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                preview = item.optString("content", "");
                status = item.optString("status", "");
                if (!preview.trim().isEmpty()) {
                    break;
                }
            }
            return new ConversationSummary(sessionId, title, preview, status, messages.length(), updatedAt);
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("sessionId", sessionId);
                json.put("title", title);
                json.put("createdAt", createdAt);
                json.put("updatedAt", updatedAt);
                json.put("messages", messages);
                json.put("tags", tags);
            } catch (Exception ignored) {
            }
            return json;
        }

        public static ConversationSnapshot fromJson(JSONObject json) {
            return new ConversationSnapshot(
                    json.optString("sessionId"),
                    json.optString("title"),
                    json.optLong("createdAt"),
                    json.optLong("updatedAt"),
                    json.optJSONArray("messages"),
                    json.optJSONArray("tags")
            );
        }
    }

    public static final class ConversationMessage {
        public final String role;
        public final String content;
        public final long createdAt;
        public final JSONArray tools;
        public final String reasoning;
        public final String status;

        public ConversationMessage(String role, String content, long createdAt, JSONArray tools, String reasoning, String status) {
            this.role = role == null ? "assistant" : role;
            this.content = safeContent(content);
            this.createdAt = createdAt <= 0 ? System.currentTimeMillis() : createdAt;
            this.tools = safeArray(tools);
            this.reasoning = safeContent(reasoning);
            this.status = status == null ? "" : status;
        }

        public static ConversationMessage user(String content) {
            return new ConversationMessage("user", content, System.currentTimeMillis(), new JSONArray(), "", "sent");
        }

        public static ConversationMessage assistant(String content, JSONArray tools, String reasoning, String status) {
            return new ConversationMessage("assistant", content, System.currentTimeMillis(), tools, reasoning, status == null ? "done" : status);
        }

        public static ConversationMessage system(String content) {
            return new ConversationMessage("system", content, System.currentTimeMillis(), new JSONArray(), "", "notice");
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("role", role);
                json.put("content", content);
                json.put("createdAt", createdAt);
                json.put("tools", tools);
                json.put("reasoning", reasoning);
                json.put("status", status);
            } catch (Exception ignored) {
            }
            return json;
        }

        public static ConversationMessage fromJson(JSONObject json) {
            return new ConversationMessage(
                    json.optString("role"),
                    json.optString("content"),
                    json.optLong("createdAt"),
                    json.optJSONArray("tools"),
                    json.optString("reasoning"),
                    json.optString("status")
            );
        }
    }

    public static final class ConversationSummary {
        public final String sessionId;
        public final String title;
        public final String preview;
        public final String status;
        public final int messageCount;
        public final long updatedAt;

        public ConversationSummary(String sessionId, String title, String preview, String status, int messageCount, long updatedAt) {
            this.sessionId = sessionId;
            this.title = safeTitle(title, "FlowMind conversation");
            this.preview = preview == null ? "" : preview;
            this.status = status == null ? "" : status;
            this.messageCount = messageCount;
            this.updatedAt = updatedAt;
        }

        public String displayTime() {
            return formatTime(updatedAt);
        }

        public String compactPreview() {
            String value = preview.replace('\n', ' ').trim();
            if (value.length() > 90) {
                return value.substring(0, 90) + "...";
            }
            return value;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sessionId", sessionId);
            map.put("title", title);
            map.put("preview", compactPreview());
            map.put("status", status);
            map.put("messageCount", messageCount);
            map.put("updatedAt", updatedAt);
            map.put("displayTime", displayTime());
            return map;
        }
    }
}

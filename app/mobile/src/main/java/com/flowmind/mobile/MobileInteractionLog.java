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

public final class MobileInteractionLog {
    private final List<Entry> entries = new ArrayList<>();
    private int maxEntries = 120;

    public void record(String scene, String action, String status) {
        record(scene, action, status, "");
    }

    public void record(String scene, String action, String status, String detail) {
        Entry entry = new Entry();
        entry.scene = scene;
        entry.action = action;
        entry.status = status;
        entry.detail = detail;
        entry.timestamp = System.currentTimeMillis();
        entries.add(0, entry);
        trim();
    }

    public void recordRequest(String scene, String method, String path) {
        record(scene, "request", "start", method + " " + path);
    }

    public void recordSuccess(String scene, String path, int itemCount) {
        record(scene, "request", "success", path + " 返回 " + itemCount + " 条数据");
    }

    public void recordFailure(String scene, String path, String message) {
        record(scene, "request", "failed", path + " " + MobileGuardrails.friendlyHttpError(message));
    }

    public List<Entry> latest(int count) {
        int end = Math.min(count, entries.size());
        return new ArrayList<>(entries.subList(0, end));
    }

    public JSONArray toJsonArray() {
        JSONArray array = new JSONArray();
        for (Entry entry : entries) array.put(entry.toJson());
        return array;
    }

    public JSONObject summary() {
        JSONObject json = new JSONObject();
        Map<String, Integer> sceneCounts = new LinkedHashMap<>();
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        try {
            for (Entry entry : entries) {
                sceneCounts.put(entry.scene, sceneCounts.getOrDefault(entry.scene, 0) + 1);
                statusCounts.put(entry.status, statusCounts.getOrDefault(entry.status, 0) + 1);
            }
            json.put("total", entries.size());
            json.put("sceneCounts", new JSONObject(sceneCounts));
            json.put("statusCounts", new JSONObject(statusCounts));
            json.put("latest", entries.isEmpty() ? new JSONObject() : entries.get(0).toJson());
        } catch (Exception ignored) {
        }
        return json;
    }

    public List<String> visibleLines() {
        List<String> lines = new ArrayList<>();
        for (Entry entry : entries) {
            lines.add(entry.visibleLine());
        }
        return lines;
    }

    public void clear() {
        entries.clear();
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = Math.max(20, maxEntries);
        trim();
    }

    private void trim() {
        while (entries.size() > maxEntries) entries.remove(entries.size() - 1);
    }

    public static final class Entry {
        public String scene = "";
        public String action = "";
        public String status = "";
        public String detail = "";
        public long timestamp;

        public String visibleLine() {
            return timeText() + " · " + scene + " · " + action + " · " + status + (detail.isBlank() ? "" : " · " + detail);
        }

        public String timeText() {
            return new SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(new Date(timestamp));
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("scene", scene);
                json.put("action", action);
                json.put("status", status);
                json.put("detail", detail);
                json.put("timestamp", timestamp);
                json.put("timeText", timeText());
            } catch (Exception ignored) {
            }
            return json;
        }
    }
}

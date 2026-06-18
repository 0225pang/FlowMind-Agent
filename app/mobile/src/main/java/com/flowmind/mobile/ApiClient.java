package com.flowmind.mobile;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ApiClient {
    interface JsonCallback {
        void onSuccess(JSONObject body);
        void onError(String message);
    }

    interface SseCallback {
        void onEvent(String event, JSONObject data);
        void onError(String message);
        void onComplete();
    }

    private final ExecutorService io = Executors.newCachedThreadPool();
    private final Handler main = new Handler(Looper.getMainLooper());
    private String baseUrl;
    private String token = "mock-jwt.demo";

    ApiClient(String baseUrl) {
        this.baseUrl = trimBase(baseUrl);
    }

    String getBaseUrl() {
        return baseUrl;
    }

    void setBaseUrl(String baseUrl) {
        this.baseUrl = trimBase(baseUrl);
    }

    void setToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            this.token = token;
        }
    }

    void get(String path, JsonCallback callback) {
        request("GET", path, null, callback);
    }

    void delete(String path, JsonCallback callback) {
        request("DELETE", path, null, callback);
    }

    void post(String path, JSONObject body, JsonCallback callback) {
        request("POST", path, body, callback);
    }

    void put(String path, JSONObject body, JsonCallback callback) {
        request("PUT", path, body, callback);
    }

    void request(String method, String path, JSONObject body, JsonCallback callback) {
        io.execute(() -> {
            HttpURLConnection conn = null;
            try {
                conn = open(method, path);
                if (body != null) {
                    conn.setDoOutput(true);
                    byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                    try (OutputStream out = conn.getOutputStream()) {
                        out.write(payload);
                    }
                }
                int status = conn.getResponseCode();
                String text = readAll(status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream());
                if (status < 200 || status >= 300) {
                    fail(callback, "HTTP " + status + ": " + text);
                    return;
                }
                JSONObject json = new JSONObject(text);
                int code = json.optInt("code", 200);
                if (code != 200) {
                    fail(callback, json.optString("message", "Request failed"));
                    return;
                }
                main.post(() -> callback.onSuccess(json));
            } catch (Exception e) {
                fail(callback, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    void postSse(String path, JSONObject body, SseCallback callback) {
        io.execute(() -> {
            HttpURLConnection conn = null;
            try {
                conn = open("POST", path);
                conn.setRequestProperty("Accept", "text/event-stream");
                conn.setReadTimeout(300_000);
                conn.setDoOutput(true);
                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(payload);
                }
                int status = conn.getResponseCode();
                if (status < 200 || status >= 300) {
                    error(callback, "HTTP " + status + ": " + readAll(conn.getErrorStream()));
                    return;
                }
                readSse(conn.getInputStream(), callback);
                main.post(callback::onComplete);
            } catch (Exception e) {
                error(callback, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private HttpURLConnection open(String method, String path) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(20_000);
        conn.setReadTimeout(60_000);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("ngrok-skip-browser-warning", "true");
        return conn;
    }

    private void readSse(InputStream input, SseCallback callback) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            String event = "message";
            StringBuilder data = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (data.length() > 0) {
                        String dataText = data.toString().trim();
                        JSONObject json;
                        try {
                            json = new JSONObject(dataText);
                        } catch (Exception ignored) {
                            json = new JSONObject().put("content", dataText);
                        }
                        String eventName = event;
                        JSONObject eventData = json;
                        main.post(() -> callback.onEvent(eventName, eventData));
                    }
                    event = "message";
                    data.setLength(0);
                    continue;
                }
                if (line.startsWith("event:")) {
                    event = line.substring("event:".length()).trim();
                } else if (line.startsWith("data:")) {
                    if (data.length() > 0) data.append('\n');
                    data.append(line.substring("data:".length()).trim());
                }
            }
        }
    }

    private String readAll(InputStream input) throws Exception {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString().trim();
    }

    private void fail(JsonCallback callback, String message) {
        main.post(() -> callback.onError(message));
    }

    private void error(SseCallback callback, String message) {
        main.post(() -> callback.onError(message));
    }

    private String trimBase(String url) {
        String value = url == null || url.trim().isEmpty() ? BuildConfig.DEFAULT_BASE_URL : url.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

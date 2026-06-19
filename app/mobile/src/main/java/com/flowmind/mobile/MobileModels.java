package com.flowmind.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class MobileModels {
    private MobileModels() {
    }

    public static final class UserProfile {
        public long id;
        public String username = "";
        public String nickname = "";
        public String role = "";
        public List<String> roles = new ArrayList<>();
        public List<String> permissions = new ArrayList<>();
        public String workspace = "";
        public int status = 1;

        public boolean admin() {
            return roles.contains("TEAM_ADMIN") || "TEAM_ADMIN".equals(role) || "ADMIN".equals(role);
        }

        public boolean hasPermission(String pattern) {
            return admin() || permissions.contains(pattern) || permissions.contains("/**");
        }

        public String displayName() {
            if (nickname != null && !nickname.isBlank()) return nickname;
            if (username != null && !username.isBlank()) return username;
            return "FlowMind 用户";
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            JSONArray roleArray = new JSONArray();
            JSONArray permissionArray = new JSONArray();
            try {
                for (String item : roles) roleArray.put(item);
                for (String item : permissions) permissionArray.put(item);
                json.put("id", id);
                json.put("username", username);
                json.put("nickname", nickname);
                json.put("role", role);
                json.put("roles", roleArray);
                json.put("permissions", permissionArray);
                json.put("workspace", workspace);
                json.put("status", status);
            } catch (Exception ignored) {
            }
            return json;
        }
    }

    public static final class ChatMessage {
        public String id = "";
        public String role = "";
        public String content = "";
        public String thinking = "";
        public String trace = "";
        public boolean streaming;
        public boolean failed;

        public String preview() {
            return MobileGuardrails.compactOneLine(content, 120);
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("id", id);
                json.put("role", role);
                json.put("content", content);
                json.put("thinking", thinking);
                json.put("trace", trace);
                json.put("streaming", streaming);
                json.put("failed", failed);
            } catch (Exception ignored) {
            }
            return json;
        }
    }

    public static final class ToolTrace {
        public String name = "";
        public String status = "";
        public String summary = "";
        public JSONObject raw = new JSONObject();

        public boolean visible() {
            return !"skipped".equalsIgnoreCase(status) && !name.isBlank();
        }

        public String label() {
            if (status == null || status.isBlank()) return name;
            return name + " / " + status;
        }
    }

    public static final class KnowledgeDoc {
        public long id;
        public String title = "";
        public String summary = "";
        public String category = "";
        public String source = "";
        public String updatedAt = "";
        public List<String> tags = new ArrayList<>();

        public String subtitle() {
            String left = category == null || category.isBlank() ? "知识文档" : category;
            String right = source == null || source.isBlank() ? "本地库" : source;
            return left + " · " + right;
        }
    }

    public static final class VectorHit {
        public String title = "";
        public String chunkText = "";
        public String source = "";
        public double distance;
        public JSONObject raw = new JSONObject();

        public String confidenceLabel() {
            if (distance <= 0) return "相关";
            if (distance < 0.25) return "高度相关";
            if (distance < 0.5) return "相关";
            return "弱相关";
        }
    }

    public static final class ContentTheme {
        public long id;
        public String title = "";
        public String summary = "";
        public String channel = "";
        public String status = "";
        public String style = "";
        public int rating;
        public int heatScore;
        public JSONObject raw = new JSONObject();

        public String statusLine() {
            List<String> parts = new ArrayList<>();
            if (!channel.isBlank()) parts.add(channel);
            if (!status.isBlank()) parts.add(status);
            if (!style.isBlank()) parts.add(style);
            if (heatScore > 0) parts.add("热度 " + heatScore);
            return String.join(" · ", parts);
        }
    }

    public static final class CopyDraft {
        public long id;
        public long themeId;
        public String title = "";
        public String content = "";
        public String channel = "";
        public String usageStatus = "";
        public String publishDate = "";
        public String imageUrl = "";
        public String imageSuggestion = "";
        public int rating;
        public JSONObject raw = new JSONObject();

        public boolean hasImage() {
            return imageUrl != null && !imageUrl.isBlank();
        }

        public String imageLine() {
            if (hasImage()) return "已登记配图";
            if (imageSuggestion != null && !imageSuggestion.isBlank()) return "配图建议：" + imageSuggestion;
            return "配图建议：校园、录取反馈、书桌复盘或材料清单类图片";
        }
    }

    public static final class CalendarItem {
        public long id;
        public long draftId;
        public long themeId;
        public String title = "";
        public String date = "";
        public String channel = "";
        public String status = "";
        public String usageStatus = "";
        public JSONObject raw = new JSONObject();

        public int day() {
            if (date == null || date.length() < 10) return -1;
            try {
                return Integer.parseInt(date.substring(8, 10));
            } catch (Exception ignored) {
                return -1;
            }
        }
    }

    public static final class SchoolInfo {
        public long id;
        public String name = "";
        public String region = "";
        public String level = "";
        public String disciplineTags = "";
        public JSONObject raw = new JSONObject();

        public String subtitle() {
            List<String> parts = new ArrayList<>();
            if (!region.isBlank()) parts.add(region);
            if (!level.isBlank()) parts.add(level);
            if (!disciplineTags.isBlank()) parts.add(disciplineTags);
            return String.join(" · ", parts);
        }
    }

    public static final class SchoolProject {
        public long id;
        public String schoolName = "";
        public String projectName = "";
        public String projectType = "";
        public String deadline = "";
        public String requirements = "";
        public String materials = "";
        public int matchScore;
        public JSONObject raw = new JSONObject();

        public String title() {
            if (!projectName.isBlank()) return projectName;
            if (!schoolName.isBlank()) return schoolName + " 项目";
            return "院校项目";
        }

        public String subtitle() {
            List<String> parts = new ArrayList<>();
            if (!schoolName.isBlank()) parts.add(schoolName);
            if (!projectType.isBlank()) parts.add(projectType);
            if (!deadline.isBlank()) parts.add("截止 " + deadline);
            if (matchScore > 0) parts.add("匹配 " + matchScore);
            return String.join(" · ", parts);
        }
    }

    public static final class FeishuStatus {
        public boolean available;
        public String mode = "";
        public String folderToken = "";
        public String message = "";
        public JSONObject raw = new JSONObject();
    }

    public static final class OperationState {
        public String scene = "";
        public String operation = "";
        public boolean loading;
        public boolean failed;
        public String message = "";
        public long startedAt;
        public long endedAt;

        public long durationMs() {
            if (startedAt <= 0) return 0;
            long end = endedAt > 0 ? endedAt : System.currentTimeMillis();
            return Math.max(0, end - startedAt);
        }
    }
}

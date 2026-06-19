package com.flowmind.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class MobileDataMappers {
    private MobileDataMappers() {
    }

    public static MobileModels.UserProfile user(JSONObject json) {
        MobileModels.UserProfile user = new MobileModels.UserProfile();
        if (json == null) return user;
        user.id = json.optLong("id", 0);
        user.username = first(json, "username", "account");
        user.nickname = first(json, "nickname", "name", "displayName");
        user.role = first(json, "role", "roleCode");
        user.workspace = first(json, "workspace", "workspaceName");
        user.status = json.optInt("status", 1);
        user.roles = stringList(json.optJSONArray("roles"));
        if (user.roles.isEmpty() && !user.role.isBlank()) user.roles.add(user.role);
        user.permissions = stringList(json.optJSONArray("permissions"));
        return user;
    }

    public static MobileModels.KnowledgeDoc knowledgeDoc(JSONObject json) {
        MobileModels.KnowledgeDoc doc = new MobileModels.KnowledgeDoc();
        if (json == null) return doc;
        doc.id = json.optLong("id", 0);
        doc.title = first(json, "title", "name", "fileName");
        doc.summary = first(json, "summary", "description", "content", "chunkText");
        doc.category = first(json, "category", "type", "feishuType");
        doc.source = first(json, "source", "sourceType", "origin");
        doc.updatedAt = first(json, "updatedAt", "updateTime", "createdAt");
        doc.tags = stringList(json.optJSONArray("tags"));
        return doc;
    }

    public static MobileModels.VectorHit vectorHit(JSONObject json) {
        MobileModels.VectorHit hit = new MobileModels.VectorHit();
        if (json == null) return hit;
        hit.title = first(json, "title", "docTitle", "name");
        hit.chunkText = first(json, "chunkText", "content", "text", "summary");
        hit.source = first(json, "source", "docSource", "origin");
        hit.distance = json.optDouble("distance", json.optDouble("score", 0));
        hit.raw = json;
        return hit;
    }

    public static MobileModels.ContentTheme theme(JSONObject json) {
        MobileModels.ContentTheme theme = new MobileModels.ContentTheme();
        if (json == null) return theme;
        theme.id = json.optLong("id", 0);
        theme.title = first(json, "title", "name", "topic");
        theme.summary = first(json, "summary", "description", "content", "note");
        theme.channel = first(json, "channel", "platform");
        theme.status = first(json, "status", "publishStatus");
        theme.style = first(json, "style", "tone");
        theme.rating = json.optInt("rating", 0);
        theme.heatScore = json.optInt("heatScore", json.optInt("heat", 0));
        theme.raw = json;
        return theme;
    }

    public static MobileModels.CopyDraft draft(JSONObject json) {
        MobileModels.CopyDraft draft = new MobileModels.CopyDraft();
        if (json == null) return draft;
        draft.id = json.optLong("id", json.optLong("draftId", 0));
        draft.themeId = json.optLong("themeId", 0);
        draft.title = first(json, "title", "name");
        draft.content = first(json, "content", "body", "text", "copy");
        draft.channel = first(json, "channel", "platform");
        draft.usageStatus = first(json, "usageStatus", "usedStatus", "status");
        draft.publishDate = first(json, "publishDate", "usedDate", "date");
        draft.imageUrl = first(json, "imageUrl", "coverUrl", "image");
        draft.imageSuggestion = first(json, "imageSuggestion", "imageAdvice", "pictureSuggestion");
        draft.rating = json.optInt("rating", 0);
        draft.raw = json;
        return draft;
    }

    public static MobileModels.CalendarItem calendarItem(JSONObject json) {
        MobileModels.CalendarItem item = new MobileModels.CalendarItem();
        if (json == null) return item;
        item.id = json.optLong("id", 0);
        item.draftId = json.optLong("draftId", 0);
        item.themeId = json.optLong("themeId", 0);
        item.title = first(json, "title", "contentTitle", "topicTitle");
        item.date = first(json, "publishDate", "date", "scheduledDate");
        item.channel = first(json, "channel", "platform");
        item.status = first(json, "status", "publishStatus");
        item.usageStatus = first(json, "usageStatus", "usedStatus");
        item.raw = json;
        return item;
    }

    public static MobileModels.SchoolInfo school(JSONObject json) {
        MobileModels.SchoolInfo school = new MobileModels.SchoolInfo();
        if (json == null) return school;
        school.id = json.optLong("id", 0);
        school.name = first(json, "name", "schoolName");
        school.region = first(json, "region", "location", "city");
        school.level = first(json, "level", "schoolLevel");
        school.disciplineTags = first(json, "disciplineTags", "tags", "majorTags");
        school.raw = json;
        return school;
    }

    public static MobileModels.SchoolProject project(JSONObject json) {
        MobileModels.SchoolProject project = new MobileModels.SchoolProject();
        if (json == null) return project;
        project.id = json.optLong("id", 0);
        project.schoolName = first(json, "schoolName", "school");
        project.projectName = first(json, "projectName", "name", "title");
        project.projectType = first(json, "projectType", "type");
        project.deadline = first(json, "deadline", "endDate", "applyDeadline");
        project.requirements = first(json, "requirements", "condition", "requirement");
        project.materials = first(json, "materials", "materialRequirements");
        project.matchScore = json.optInt("matchScore", json.optInt("score", 0));
        project.raw = json;
        return project;
    }

    public static MobileModels.FeishuStatus feishuStatus(JSONObject json) {
        MobileModels.FeishuStatus status = new MobileModels.FeishuStatus();
        if (json == null) return status;
        status.available = json.optBoolean("available", json.optBoolean("enabled", true));
        status.mode = first(json, "mode", "clientMode");
        status.folderToken = first(json, "folderToken", "knowledgeBaseFolderToken");
        status.message = first(json, "message", "status", "desc");
        status.raw = json;
        return status;
    }

    public static List<MobileModels.KnowledgeDoc> knowledgeDocs(JSONArray array) {
        List<MobileModels.KnowledgeDoc> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) list.add(knowledgeDoc(array.optJSONObject(i)));
        return list;
    }

    public static List<MobileModels.VectorHit> vectorHits(JSONArray array) {
        List<MobileModels.VectorHit> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) list.add(vectorHit(array.optJSONObject(i)));
        return list;
    }

    public static List<MobileModels.ContentTheme> themes(JSONArray array) {
        List<MobileModels.ContentTheme> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) list.add(theme(array.optJSONObject(i)));
        return list;
    }

    public static List<MobileModels.CopyDraft> drafts(JSONArray array) {
        List<MobileModels.CopyDraft> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) list.add(draft(array.optJSONObject(i)));
        return list;
    }

    public static List<MobileModels.CalendarItem> calendar(JSONArray array) {
        List<MobileModels.CalendarItem> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) list.add(calendarItem(array.optJSONObject(i)));
        return list;
    }

    public static List<MobileModels.SchoolInfo> schools(JSONArray array) {
        List<MobileModels.SchoolInfo> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) list.add(school(array.optJSONObject(i)));
        return list;
    }

    public static List<MobileModels.SchoolProject> projects(JSONArray array) {
        List<MobileModels.SchoolProject> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) list.add(project(array.optJSONObject(i)));
        return list;
    }

    public static JSONArray toArray(List<? extends Object> values) {
        JSONArray array = new JSONArray();
        if (values == null) return array;
        for (Object value : values) {
            if (value instanceof MobileModels.KnowledgeDoc) array.put(toJson((MobileModels.KnowledgeDoc) value));
            else if (value instanceof MobileModels.VectorHit) array.put(toJson((MobileModels.VectorHit) value));
            else if (value instanceof MobileModels.ContentTheme) array.put(toJson((MobileModels.ContentTheme) value));
            else if (value instanceof MobileModels.CopyDraft) array.put(toJson((MobileModels.CopyDraft) value));
            else if (value instanceof MobileModels.SchoolProject) array.put(toJson((MobileModels.SchoolProject) value));
            else array.put(String.valueOf(value));
        }
        return array;
    }

    public static JSONObject toJson(MobileModels.KnowledgeDoc doc) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", doc.id);
            json.put("title", doc.title);
            json.put("summary", doc.summary);
            json.put("category", doc.category);
            json.put("source", doc.source);
            json.put("updatedAt", doc.updatedAt);
            json.put("tags", new JSONArray(doc.tags));
        } catch (Exception ignored) {
        }
        return json;
    }

    public static JSONObject toJson(MobileModels.VectorHit hit) {
        JSONObject json = new JSONObject();
        try {
            json.put("title", hit.title);
            json.put("chunkText", hit.chunkText);
            json.put("source", hit.source);
            json.put("distance", hit.distance);
            json.put("confidence", hit.confidenceLabel());
        } catch (Exception ignored) {
        }
        return json;
    }

    public static JSONObject toJson(MobileModels.ContentTheme theme) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", theme.id);
            json.put("title", theme.title);
            json.put("summary", theme.summary);
            json.put("channel", theme.channel);
            json.put("status", theme.status);
            json.put("style", theme.style);
            json.put("rating", theme.rating);
            json.put("heatScore", theme.heatScore);
        } catch (Exception ignored) {
        }
        return json;
    }

    public static JSONObject toJson(MobileModels.CopyDraft draft) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", draft.id);
            json.put("themeId", draft.themeId);
            json.put("title", draft.title);
            json.put("content", draft.content);
            json.put("channel", draft.channel);
            json.put("usageStatus", draft.usageStatus);
            json.put("publishDate", draft.publishDate);
            json.put("imageUrl", draft.imageUrl);
            json.put("imageSuggestion", draft.imageSuggestion);
            json.put("rating", draft.rating);
        } catch (Exception ignored) {
        }
        return json;
    }

    public static JSONObject toJson(MobileModels.SchoolProject project) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", project.id);
            json.put("schoolName", project.schoolName);
            json.put("projectName", project.projectName);
            json.put("projectType", project.projectType);
            json.put("deadline", project.deadline);
            json.put("requirements", project.requirements);
            json.put("materials", project.materials);
            json.put("matchScore", project.matchScore);
        } catch (Exception ignored) {
        }
        return json;
    }

    public static String first(JSONObject json, String... keys) {
        if (json == null || keys == null) return "";
        for (String key : keys) {
            String value = json.optString(key, "");
            if (value != null && !value.trim().isEmpty() && !"null".equalsIgnoreCase(value)) return value;
        }
        return "";
    }

    private static List<String> stringList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, "");
            if (!value.isBlank()) list.add(value);
        }
        return list;
    }
}

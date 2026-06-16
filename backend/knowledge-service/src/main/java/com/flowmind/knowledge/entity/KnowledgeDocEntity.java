package com.flowmind.knowledge.entity;

import java.time.LocalDateTime;

public class KnowledgeDocEntity {
    private Long id;
    private String feishuToken;
    private String title;
    private String content;
    private String summary;
    private String tags;           // JSON array string, e.g. ["夏令营","材料"]
    private String feishuUrl;
    private String feishuType;     // docx, doc, sheet, bitable, folder, file
    private long feishuModifiedAt; // Unix timestamp from Feishu, used for change detection
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFeishuToken() { return feishuToken; }
    public void setFeishuToken(String feishuToken) { this.feishuToken = feishuToken; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getFeishuUrl() { return feishuUrl; }
    public void setFeishuUrl(String feishuUrl) { this.feishuUrl = feishuUrl; }

    public String getFeishuType() { return feishuType; }
    public void setFeishuType(String feishuType) { this.feishuType = feishuType; }

    public long getFeishuModifiedAt() { return feishuModifiedAt; }
    public void setFeishuModifiedAt(long feishuModifiedAt) { this.feishuModifiedAt = feishuModifiedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

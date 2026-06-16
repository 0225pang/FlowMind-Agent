package com.flowmind.knowledge.entity;

import java.time.LocalDateTime;

public class SyncLogEntity {
    private Long id;
    private String syncType;        // docs / bitable / tasks / bot
    private String status;          // SUCCESS / FAILED / PARTIAL
    private String message;         // human-readable summary
    private int added;
    private int updated;
    private int skipped;
    private int errors;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getAdded() { return added; }
    public void setAdded(int added) { this.added = added; }

    public int getUpdated() { return updated; }
    public void setUpdated(int updated) { this.updated = updated; }

    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }

    public int getErrors() { return errors; }
    public void setErrors(int errors) { this.errors = errors; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

package com.flowmind.user.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserEntity {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private Integer status = 1;
    private List<String> roles = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted = false;

    public UserEntity() {
    }

    public UserEntity(Long id, String username, String password, String nickname, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.roles = new ArrayList<>(List.of(role));
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles == null ? new ArrayList<>() : roles; }
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions == null ? new ArrayList<>() : permissions; }
    public String getRole() { return roles == null || roles.isEmpty() ? "" : roles.get(0); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}

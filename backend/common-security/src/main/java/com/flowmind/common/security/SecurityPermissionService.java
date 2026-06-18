package com.flowmind.common.security;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class SecurityPermissionService {
    private final JdbcTemplate jdbcTemplate;

    public SecurityPermissionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AccessDecision decide(String username, String method, String uri) {
        try {
            List<String> roles = rolesOf(username);
            if (roles.isEmpty()) {
                return AccessDecision.deny("用户不存在或未分配角色");
            }
            if (roles.contains("TEAM_ADMIN")) {
                return AccessDecision.allow(roles, permissionsOfRoles(roles));
            }
            List<String> permissions = permissionsOfRoles(roles);
            boolean allowed = permissions.stream().anyMatch(pattern -> match(pattern, uri));
            if (!allowed) {
                return AccessDecision.deny("当前角色无权访问：" + uri, roles, permissions);
            }
            return AccessDecision.allow(roles, permissions);
        } catch (DataAccessException ex) {
            return AccessDecision.deny("权限表未初始化或数据库不可用：" + ex.getMessage());
        }
    }

    public List<String> rolesOf(String username) {
        return jdbcTemplate.queryForList("""
                select r.role_code
                from sys_user u
                join sys_user_role ur on ur.user_id = u.id and ur.deleted = 0
                join sys_role r on r.id = ur.role_id and r.deleted = 0
                where u.username = ? and u.deleted = 0 and u.status = 1
                order by r.id
                """, String.class, username);
    }

    public List<String> permissionsOfRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", roles.stream().map(r -> "?").toList());
        List<String> values = jdbcTemplate.queryForList("""
                select p.path_pattern
                from sys_role r
                join sys_role_permission rp on rp.role_id = r.id and rp.deleted = 0
                join sys_permission p on p.id = rp.permission_id and p.deleted = 0 and p.enabled = 1
                where r.deleted = 0 and r.role_code in (%s)
                group by p.path_pattern
                order by min(p.id)
                """.formatted(placeholders), String.class, roles.toArray());
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    private boolean match(String pattern, String uri) {
        if (pattern == null || pattern.isBlank()) {
            return false;
        }
        if ("/**".equals(pattern)) {
            return true;
        }
        if (pattern.endsWith("/**")) {
            return uri.startsWith(pattern.substring(0, pattern.length() - 3));
        }
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            if (!uri.startsWith(prefix)) {
                return false;
            }
            return uri.substring(prefix.length()).indexOf('/') < 0;
        }
        return uri.equals(pattern);
    }

    public record AccessDecision(boolean allowed, String message, List<String> roles, List<String> permissions) {
        public static AccessDecision allow(List<String> roles, List<String> permissions) {
            return new AccessDecision(true, "success", roles, permissions);
        }

        public static AccessDecision deny(String message) {
            return new AccessDecision(false, message, List.of(), List.of());
        }

        public static AccessDecision deny(String message, List<String> roles, List<String> permissions) {
            return new AccessDecision(false, message, roles == null ? List.of() : roles, permissions == null ? List.of() : permissions);
        }
    }
}

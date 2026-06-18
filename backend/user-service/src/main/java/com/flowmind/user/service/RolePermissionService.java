package com.flowmind.user.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RolePermissionService {
    private final JdbcTemplate jdbcTemplate;

    public RolePermissionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> roles() {
        List<Map<String, Object>> roles = jdbcTemplate.queryForList("""
                select id, role_code roleCode, role_name roleName, created_at createdAt, updated_at updatedAt
                from sys_role
                where deleted = 0
                order by id
                """);
        for (Map<String, Object> role : roles) {
            role.put("permissions", permissionsOfRole(String.valueOf(role.get("roleCode"))));
            role.put("editable", !"TEAM_ADMIN".equals(role.get("roleCode")));
        }
        return roles;
    }

    public List<Map<String, Object>> permissions() {
        return jdbcTemplate.queryForList("""
                select id, permission_code permissionCode, permission_name permissionName,
                       path_pattern pathPattern, frontend_route frontendRoute, description, enabled
                from sys_permission
                where deleted = 0
                order by id
                """);
    }

    public List<Map<String, Object>> permissionsOfRole(String roleCode) {
        return jdbcTemplate.queryForList("""
                select p.permission_code permissionCode, p.permission_name permissionName,
                       p.path_pattern pathPattern, p.frontend_route frontendRoute, p.description, p.enabled
                from sys_role r
                join sys_role_permission rp on rp.role_id = r.id and rp.deleted = 0
                join sys_permission p on p.id = rp.permission_id and p.deleted = 0
                where r.role_code = ? and r.deleted = 0
                order by p.id
                """, roleCode);
    }

    @Transactional
    public Map<String, Object> updateRolePermissions(String roleCode, List<String> permissionCodes) {
        if ("TEAM_ADMIN".equals(roleCode) && (permissionCodes == null || permissionCodes.isEmpty())) {
            throw new IllegalArgumentException("团队管理员不能被清空权限");
        }
        Long roleId = jdbcTemplate.queryForObject("select id from sys_role where role_code = ? and deleted = 0 limit 1", Long.class, roleCode);
        jdbcTemplate.update("update sys_role_permission set deleted = 1 where role_id = ?", roleId);
        if (permissionCodes != null) {
            for (String code : permissionCodes) {
                Long permissionId = jdbcTemplate.queryForObject("select id from sys_permission where permission_code = ? and deleted = 0 limit 1", Long.class, code);
                jdbcTemplate.update("insert into sys_role_permission(role_id, permission_id, deleted) values (?, ?, 0)", roleId, permissionId);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roleCode", roleCode);
        result.put("permissions", permissionsOfRole(roleCode));
        result.put("message", "角色权限已更新，用户下次请求立即生效");
        return result;
    }
}

package com.flowmind.user.security;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RbacDataInitializer {
    private final JdbcTemplate jdbcTemplate;

    public RbacDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        createTables();
        seedRoles();
        seedPermissions();
        seedUsers();
        seedRolePermissions();
        seedUserRoles();
    }

    private void createTables() {
        jdbcTemplate.execute("""
                create table if not exists sys_user (
                    id bigint primary key auto_increment,
                    username varchar(64) not null,
                    password varchar(128) not null,
                    nickname varchar(64),
                    avatar varchar(255),
                    status tinyint default 1,
                    created_at datetime default current_timestamp,
                    updated_at datetime default current_timestamp on update current_timestamp,
                    deleted tinyint default 0
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists sys_role (
                    id bigint primary key auto_increment,
                    role_code varchar(64) not null,
                    role_name varchar(64) not null,
                    created_at datetime default current_timestamp,
                    updated_at datetime default current_timestamp on update current_timestamp,
                    deleted tinyint default 0
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists sys_user_role (
                    id bigint primary key auto_increment,
                    user_id bigint not null,
                    role_id bigint not null,
                    created_at datetime default current_timestamp,
                    updated_at datetime default current_timestamp on update current_timestamp,
                    deleted tinyint default 0
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists sys_permission (
                    id bigint primary key auto_increment,
                    permission_code varchar(96) not null,
                    permission_name varchar(96) not null,
                    path_pattern varchar(160) not null,
                    frontend_route varchar(96),
                    description varchar(255),
                    enabled tinyint default 1,
                    created_at datetime default current_timestamp,
                    updated_at datetime default current_timestamp on update current_timestamp,
                    deleted tinyint default 0
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists sys_role_permission (
                    id bigint primary key auto_increment,
                    role_id bigint not null,
                    permission_id bigint not null,
                    created_at datetime default current_timestamp,
                    updated_at datetime default current_timestamp on update current_timestamp,
                    deleted tinyint default 0
                )
                """);
    }

    private void seedRoles() {
        upsertRole("CONTENT_OPERATOR", "内容运营人员");
        upsertRole("EDU_CONSULTANT", "教育咨询老师");
        upsertRole("IP_OPERATOR", "个人IP运营者");
        upsertRole("TEAM_ADMIN", "团队管理员");
        upsertRole("STUDENT_USER", "学员用户");
    }

    private void seedPermissions() {
        upsertPermission("PAGE_DASHBOARD", "Dashboard", "/api/analytics/**", "/dashboard", "查看工作台总览和统计数据");
        upsertPermission("PAGE_AGENT", "AI 工作台", "/api/agents/**", "/agent", "访问总智能体、会话和流式对话");
        upsertPermission("PAGE_PROMPT", "Prompt 模板", "/api/prompts/**", "/agent", "查看和维护 Prompt 模板");
        upsertPermission("PAGE_KNOWLEDGE", "知识库", "/api/knowledge/**", "/knowledge", "查看知识库文档、标签和向量检索");
        upsertPermission("PAGE_CONTENT", "内容运营", "/api/content/**", "/content", "查看和维护主题库、文案库和内容日历");
        upsertPermission("PAGE_STUDENTS", "学员管理", "/api/students/**", "/students", "查看和维护学员画像、进度和风险等级");
        upsertPermission("PAGE_SCHOOLS", "院校情报", "/api/schools/**", "/schools", "查看学校信息和执行院校推荐");
        upsertPermission("PAGE_SCHOOL_PROJECTS", "院校项目", "/api/school-projects/**", "/schools", "查看和维护夏令营、预推免项目");
        upsertPermission("PAGE_FEISHU", "飞书同步", "/api/feishu/**", "/feishu", "查看飞书同步状态、知识库文件和机器人日志");
        upsertPermission("PAGE_SETTINGS", "系统设置", "/api/users/**", "/settings", "查看当前用户和基础配置");
        upsertPermission("RBAC_ROLE_MANAGE", "角色权限配置", "/api/roles/**", "/settings", "团队管理员维护角色权限");
        upsertPermission("RBAC_PERMISSION_VIEW", "权限清单查看", "/api/permissions/**", "/settings", "团队管理员查看权限点清单");
        upsertPermission("GATEWAY_ROUTE_VIEW", "网关路由", "/api/gateway/**", "/settings", "查看服务路由与网关信息");
    }

    private void seedUsers() {
        upsertUser("admin", "123456", "团队管理员 Admin");
        upsertUser("content", "123456", "内容运营人员");
        upsertUser("teacher", "123456", "教育咨询老师");
        upsertUser("ip", "123456", "个人IP运营者");
        upsertUser("student", "123456", "学员用户");
        upsertUser("demo", "123456", "兼容演示账号");
    }

    private void seedRolePermissions() {
        List<String> allBusiness = List.of(
                "PAGE_DASHBOARD", "PAGE_AGENT", "PAGE_PROMPT", "PAGE_KNOWLEDGE", "PAGE_CONTENT",
                "PAGE_STUDENTS", "PAGE_SCHOOLS", "PAGE_SCHOOL_PROJECTS", "PAGE_FEISHU", "PAGE_SETTINGS", "GATEWAY_ROUTE_VIEW"
        );
        List<String> student = List.of("PAGE_AGENT", "PAGE_KNOWLEDGE", "PAGE_SCHOOLS", "PAGE_SCHOOL_PROJECTS", "PAGE_SETTINGS");
        List<String> admin = List.of(
                "PAGE_DASHBOARD", "PAGE_AGENT", "PAGE_PROMPT", "PAGE_KNOWLEDGE", "PAGE_CONTENT",
                "PAGE_STUDENTS", "PAGE_SCHOOLS", "PAGE_SCHOOL_PROJECTS", "PAGE_FEISHU", "PAGE_SETTINGS",
                "RBAC_ROLE_MANAGE", "RBAC_PERMISSION_VIEW", "GATEWAY_ROUTE_VIEW"
        );
        grant("TEAM_ADMIN", admin);
        grant("CONTENT_OPERATOR", allBusiness);
        grant("EDU_CONSULTANT", allBusiness);
        grant("IP_OPERATOR", allBusiness);
        grant("STUDENT_USER", student);
    }

    private void seedUserRoles() {
        bindUserRole("admin", "TEAM_ADMIN");
        bindUserRole("content", "CONTENT_OPERATOR");
        bindUserRole("teacher", "EDU_CONSULTANT");
        bindUserRole("ip", "IP_OPERATOR");
        bindUserRole("student", "STUDENT_USER");
        bindUserRole("demo", "CONTENT_OPERATOR");
    }

    private void upsertRole(String code, String name) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from sys_role where role_code = ? and deleted = 0", Integer.class, code);
        if (count == null || count == 0) {
            jdbcTemplate.update("insert into sys_role(role_code, role_name) values (?, ?)", code, name);
        } else {
            jdbcTemplate.update("update sys_role set role_name = ? where role_code = ? and deleted = 0", name, code);
        }
    }

    private void upsertPermission(String code, String name, String path, String route, String description) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from sys_permission where permission_code = ? and deleted = 0", Integer.class, code);
        if (count == null || count == 0) {
            jdbcTemplate.update("""
                    insert into sys_permission(permission_code, permission_name, path_pattern, frontend_route, description, enabled)
                    values (?, ?, ?, ?, ?, 1)
                    """, code, name, path, route, description);
        } else {
            jdbcTemplate.update("""
                    update sys_permission
                    set permission_name = ?, path_pattern = ?, frontend_route = ?, description = ?, enabled = 1
                    where permission_code = ? and deleted = 0
                    """, name, path, route, description, code);
        }
    }

    private void upsertUser(String username, String password, String nickname) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from sys_user where username = ? and deleted = 0", Integer.class, username);
        if (count == null || count == 0) {
            jdbcTemplate.update("insert into sys_user(username, password, nickname, status) values (?, ?, ?, 1)", username, password, nickname);
        } else {
            jdbcTemplate.update("update sys_user set password = ?, nickname = ?, status = 1 where username = ? and deleted = 0", password, nickname, username);
        }
    }

    private void grant(String roleCode, List<String> permissionCodes) {
        Long roleId = roleId(roleCode);
        for (String permissionCode : permissionCodes) {
            Long permissionId = permissionId(permissionCode);
            Integer count = jdbcTemplate.queryForObject("""
                    select count(*) from sys_role_permission
                    where role_id = ? and permission_id = ? and deleted = 0
                    """, Integer.class, roleId, permissionId);
            if (count == null || count == 0) {
                jdbcTemplate.update("insert into sys_role_permission(role_id, permission_id) values (?, ?)", roleId, permissionId);
            }
        }
    }

    private void bindUserRole(String username, String roleCode) {
        Long userId = userId(username);
        Long roleId = roleId(roleCode);
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from sys_user_role
                where user_id = ? and role_id = ? and deleted = 0
                """, Integer.class, userId, roleId);
        if (count == null || count == 0) {
            jdbcTemplate.update("insert into sys_user_role(user_id, role_id) values (?, ?)", userId, roleId);
        }
    }

    private Long userId(String username) {
        return jdbcTemplate.queryForObject("select id from sys_user where username = ? and deleted = 0 limit 1", Long.class, username);
    }

    private Long roleId(String code) {
        return jdbcTemplate.queryForObject("select id from sys_role where role_code = ? and deleted = 0 limit 1", Long.class, code);
    }

    private Long permissionId(String code) {
        return jdbcTemplate.queryForObject("select id from sys_permission where permission_code = ? and deleted = 0 limit 1", Long.class, code);
    }
}

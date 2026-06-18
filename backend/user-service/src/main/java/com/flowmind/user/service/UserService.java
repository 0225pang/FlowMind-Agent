package com.flowmind.user.service;

import com.flowmind.common.security.TokenUtil;
import com.flowmind.user.dto.LoginRequest;
import com.flowmind.user.entity.UserEntity;
import com.flowmind.user.mapper.UserMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserService {
    private final UserMapper mapper;
    private final JdbcTemplate jdbcTemplate;

    public UserService(UserMapper mapper, JdbcTemplate jdbcTemplate) {
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> login(LoginRequest req) {
        if (req == null || req.getUsername() == null || req.getUsername().isBlank()) {
            throw new IllegalArgumentException("请输入账号");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("请输入密码");
        }
        UserEntity user = mapper.findByUsername(req.getUsername())
                .filter(u -> u.getPassword().equals(req.getPassword()))
                .orElseThrow(() -> new IllegalArgumentException("账号或密码错误"));
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new IllegalArgumentException("账号已被禁用，请联系团队管理员");
        }
        return Map.of(
                "token", TokenUtil.createMockToken(user.getUsername()),
                "user", toUserView(user)
        );
    }

    public UserEntity register(LoginRequest req) {
        UserEntity user = mapper.save(new UserEntity(null, req.getUsername(), req.getPassword(), req.getUsername(), "STUDENT_USER"));
        bindDefaultStudentRole(user.getId());
        return user;
    }

    public Map<String, Object> me(String username) {
        UserEntity user = mapper.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("当前登录用户不存在: " + username));
        Map<String, Object> view = toUserView(user);
        view.put("workspace", "保研内容运营工作空间");
        return view;
    }

    private Map<String, Object> toUserView(UserEntity user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId());
        view.put("username", user.getUsername());
        view.put("nickname", user.getNickname());
        view.put("avatar", user.getAvatar());
        view.put("role", user.getRole());
        view.put("roles", user.getRoles());
        view.put("permissions", user.getPermissions());
        view.put("status", user.getStatus());
        return view;
    }

    private void bindDefaultStudentRole(Long userId) {
        Long roleId = jdbcTemplate.queryForObject("select id from sys_role where role_code = 'STUDENT_USER' and deleted = 0 limit 1", Long.class);
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from sys_user_role where user_id = ? and role_id = ? and deleted = 0
                """, Integer.class, userId, roleId);
        if (count == null || count == 0) {
            jdbcTemplate.update("insert into sys_user_role(user_id, role_id) values (?, ?)", userId, roleId);
        }
    }
}

package com.flowmind.user.mapper;

import com.flowmind.user.entity.UserEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

@Repository
public class UserMapper {
    private final JdbcTemplate jdbcTemplate;

    public UserMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserEntity> findByUsername(String username) {
        List<UserEntity> users = jdbcTemplate.query("""
                select id, username, password, nickname, avatar, status, created_at, updated_at, deleted
                from sys_user
                where username = ? and deleted = 0
                limit 1
                """, mapper(), username);
        if (users.isEmpty()) {
            return Optional.empty();
        }
        UserEntity user = users.get(0);
        user.setRoles(findRoles(user.getId()));
        user.setPermissions(findPermissions(user.getId()));
        return Optional.of(user);
    }

    public UserEntity save(UserEntity user) {
        jdbcTemplate.update("""
                insert into sys_user(username, password, nickname, avatar, status, deleted)
                values (?, ?, ?, ?, 1, 0)
                """, user.getUsername(), user.getPassword(), user.getNickname(), user.getAvatar());
        return findByUsername(user.getUsername()).orElse(user);
    }

    public List<String> findRoles(Long userId) {
        return jdbcTemplate.queryForList("""
                select r.role_code
                from sys_user_role ur
                join sys_role r on r.id = ur.role_id and r.deleted = 0
                where ur.user_id = ? and ur.deleted = 0
                order by r.id
                """, String.class, userId);
    }

    public List<String> findPermissions(Long userId) {
        return jdbcTemplate.queryForList("""
                select p.path_pattern
                from sys_user_role ur
                join sys_role_permission rp on rp.role_id = ur.role_id and rp.deleted = 0
                join sys_permission p on p.id = rp.permission_id and p.deleted = 0 and p.enabled = 1
                where ur.user_id = ? and ur.deleted = 0
                group by p.path_pattern
                order by min(p.id)
                """, String.class, userId);
    }

    private RowMapper<UserEntity> mapper() {
        return (ResultSet rs, int rowNum) -> {
            UserEntity user = new UserEntity();
            user.setId(rs.getLong("id"));
            user.setUsername(rs.getString("username"));
            user.setPassword(rs.getString("password"));
            user.setNickname(rs.getString("nickname"));
            user.setAvatar(rs.getString("avatar"));
            user.setStatus(rs.getInt("status"));
            user.setCreatedAt(rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime());
            user.setUpdatedAt(rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
            user.setDeleted(rs.getBoolean("deleted"));
            return user;
        };
    }
}

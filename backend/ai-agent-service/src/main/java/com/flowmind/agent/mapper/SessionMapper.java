package com.flowmind.agent.mapper;

import com.flowmind.agent.entity.AgentSessionEntity;
import com.flowmind.common.core.IdGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class SessionMapper {
    private final JdbcTemplate jdbcTemplate;

    public SessionMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        createTableIfNeeded();
    }

    private void createTableIfNeeded() {
        jdbcTemplate.execute("""
                create table if not exists agent_session (
                    id bigint primary key auto_increment,
                    session_id varchar(60) not null comment 'session uuid',
                    agent_type varchar(40) not null comment 'agent type',
                    title varchar(200) null comment 'first user message as title',
                    turn_count int not null default 0,
                    created_at datetime not null default current_timestamp,
                    updated_at datetime not null default current_timestamp on update current_timestamp,
                    unique key uk_session_id (session_id),
                    key idx_session_agent_updated (agent_type, updated_at desc),
                    key idx_session_created (created_at desc)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='Agent sessions'
                """);
    }

    public void upsert(String sessionId, String agentType, String title, int turnCount) {
        try {
            long id = IdGenerator.nextId() + System.nanoTime() % 100000;
            jdbcTemplate.update("""
                    insert into agent_session(id, session_id, agent_type, title, turn_count)
                    values (?, ?, ?, ?, ?)
                    """, id, sessionId, agentType, title, turnCount);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // Session already exists, update it
        }
        jdbcTemplate.update("""
                update agent_session set title = coalesce(?, title), turn_count = ?, updated_at = current_timestamp
                where session_id = ?
                """, title != null ? title : null, turnCount, sessionId);
    }

    public List<AgentSessionEntity> listSessions() {
        return jdbcTemplate.query("""
                select id, session_id, agent_type, title, turn_count, created_at, updated_at
                from agent_session
                order by updated_at desc
                limit 50
                """, rowMapper());
    }

    public void delete(String sessionId) {
        jdbcTemplate.update("delete from agent_session where session_id = ?", sessionId);
    }

    private RowMapper<AgentSessionEntity> rowMapper() {
        return (rs, rowNum) -> {
            AgentSessionEntity e = new AgentSessionEntity();
            e.setId(rs.getLong("id"));
            e.setSessionId(rs.getString("session_id"));
            e.setAgentType(rs.getString("agent_type"));
            e.setTitle(rs.getString("title"));
            e.setTurnCount(rs.getInt("turn_count"));
            Timestamp ca = rs.getTimestamp("created_at");
            if (ca != null) e.setCreatedAt(ca.toLocalDateTime());
            Timestamp ua = rs.getTimestamp("updated_at");
            if (ua != null) e.setUpdatedAt(ua.toLocalDateTime());
            return e;
        };
    }
}

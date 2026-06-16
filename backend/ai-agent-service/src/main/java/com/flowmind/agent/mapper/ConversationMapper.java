package com.flowmind.agent.mapper;

import com.flowmind.agent.entity.ConversationEntity;
import com.flowmind.common.core.IdGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ConversationMapper {
    private final JdbcTemplate jdbcTemplate;

    public ConversationMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        createTableIfNeeded();
    }

    private void createTableIfNeeded() {
        jdbcTemplate.execute("""
                create table if not exists agent_conversation (
                    id bigint primary key auto_increment,
                    agent_type varchar(40) not null comment 'agent type: content/feishu/knowledge/student/school',
                    session_id varchar(60) not null comment 'session uuid',
                    turn_index int not null default 0 comment 'turn number within session',
                    role varchar(12) not null comment 'user | assistant',
                    content mediumtext not null comment 'message content',
                    created_at datetime not null default current_timestamp,
                    key idx_conversation_session (session_id, agent_type),
                    key idx_conversation_agent_turn (agent_type, turn_index),
                    key idx_conversation_created (created_at)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='Agent conversation history'
                """);
    }

    public ConversationEntity insert(String agentType, String sessionId, int turnIndex,
                                      String role, String content) {
        long id = IdGenerator.nextId();
        try {
            jdbcTemplate.update("""
                    insert into agent_conversation(id, agent_type, session_id, turn_index, role, content)
                    values (?, ?, ?, ?, ?, ?)
                    """, id, agentType, sessionId, turnIndex, role, content);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // IdGenerator collision with existing auto_increment values — retry once
            id = IdGenerator.nextId() + System.nanoTime() % 100000;
            jdbcTemplate.update("""
                    insert into agent_conversation(id, agent_type, session_id, turn_index, role, content)
                    values (?, ?, ?, ?, ?, ?)
                    """, id, agentType, sessionId, turnIndex, role, content);
        }
        return findById(id);
    }

    public ConversationEntity findById(Long id) {
        return jdbcTemplate.queryForObject(
                "select id, agent_type, session_id, turn_index, role, content, created_at from agent_conversation where id = ?",
                rowMapper(), id);
    }

    /**
     * Load recent turns for a session (up to limit, ordered by turn_index).
     */
    public List<ConversationEntity> loadRecentTurns(String agentType, String sessionId, int limit) {
        return jdbcTemplate.query("""
                select id, agent_type, session_id, turn_index, role, content, created_at
                from agent_conversation
                where agent_type = ? and session_id = ?
                order by turn_index desc
                limit ?
                """, rowMapper(), agentType, sessionId, limit);
    }

    /**
     * Soft-delete all turns for a session.
     */
    public void deleteSession(String agentType, String sessionId) {
        jdbcTemplate.update(
                "delete from agent_conversation where agent_type = ? and session_id = ?",
                agentType, sessionId);
    }

    /**
     * Load ALL turns for a session (for full context).
     */
    public List<ConversationEntity> loadAllTurns(String agentType, String sessionId) {
        return jdbcTemplate.query("""
                select id, agent_type, session_id, turn_index, role, content, created_at
                from agent_conversation
                where agent_type = ? and session_id = ?
                order by turn_index asc
                """, rowMapper(), agentType, sessionId);
    }

    private RowMapper<ConversationEntity> rowMapper() {
        return (rs, rowNum) -> {
            ConversationEntity e = new ConversationEntity();
            e.setId(rs.getLong("id"));
            e.setAgentType(rs.getString("agent_type"));
            e.setSessionId(rs.getString("session_id"));
            e.setTurnIndex(rs.getInt("turn_index"));
            e.setRole(rs.getString("role"));
            e.setContent(rs.getString("content"));
            Timestamp ts = rs.getTimestamp("created_at");
            e.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
            return e;
        };
    }
}

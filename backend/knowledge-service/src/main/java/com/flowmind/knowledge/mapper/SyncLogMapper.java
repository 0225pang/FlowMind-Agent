package com.flowmind.knowledge.mapper;

import com.flowmind.common.core.IdGenerator;
import com.flowmind.knowledge.entity.SyncLogEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class SyncLogMapper {
    private final JdbcTemplate jdbcTemplate;

    public SyncLogMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        createTableIfNeeded();
    }

    private void createTableIfNeeded() {
        jdbcTemplate.execute("""
                create table if not exists knowledge_sync_log (
                    id bigint primary key auto_increment,
                    sync_type varchar(30) not null comment 'docs/bitable/tasks/bot',
                    status varchar(20) not null comment 'SUCCESS/FAILED/PARTIAL',
                    message varchar(500) null comment 'human-readable summary',
                    added int not null default 0,
                    updated int not null default 0,
                    skipped int not null default 0,
                    errors int not null default 0,
                    created_at datetime not null default current_timestamp,
                    key idx_sync_log_type (sync_type),
                    key idx_sync_log_created (created_at desc)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='Knowledge sync logs'
                """);
        // Migrate: if knowledge_sync_log doesn't exist yet from old schema
    }

    public void insert(SyncLogEntity log) {
        long id = IdGenerator.nextId();
        jdbcTemplate.update("""
                insert into knowledge_sync_log(id, sync_type, status, message, added, updated, skipped, errors)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, log.getSyncType(), log.getStatus(), log.getMessage(),
                log.getAdded(), log.getUpdated(), log.getSkipped(), log.getErrors());
    }

    public List<SyncLogEntity> listRecent(int limit) {
        return jdbcTemplate.query("""
                select id, sync_type, status, message, added, updated, skipped, errors, created_at
                from knowledge_sync_log
                order by created_at desc
                limit ?
                """, rowMapper(), limit);
    }

    public SyncLogEntity latestByType(String syncType) {
        List<SyncLogEntity> logs = jdbcTemplate.query("""
                select id, sync_type, status, message, added, updated, skipped, errors, created_at
                from knowledge_sync_log
                where sync_type = ?
                order by created_at desc
                limit 1
                """, rowMapper(), syncType);
        return logs.isEmpty() ? null : logs.get(0);
    }

    private RowMapper<SyncLogEntity> rowMapper() {
        return (rs, rowNum) -> {
            SyncLogEntity e = new SyncLogEntity();
            e.setId(rs.getLong("id"));
            e.setSyncType(rs.getString("sync_type"));
            e.setStatus(rs.getString("status"));
            e.setMessage(rs.getString("message"));
            e.setAdded(rs.getInt("added"));
            e.setUpdated(rs.getInt("updated"));
            e.setSkipped(rs.getInt("skipped"));
            e.setErrors(rs.getInt("errors"));
            Timestamp ca = rs.getTimestamp("created_at");
            if (ca != null) e.setCreatedAt(ca.toLocalDateTime());
            return e;
        };
    }
}

package com.flowmind.knowledge.mapper;

import com.flowmind.common.core.IdGenerator;
import com.flowmind.knowledge.entity.KnowledgeDocEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class KnowledgeMapper {
    private final JdbcTemplate jdbcTemplate;

    public KnowledgeMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        createTableIfNeeded();
    }

    private void createTableIfNeeded() {
        jdbcTemplate.execute("""
                create table if not exists knowledge_doc (
                    id bigint primary key auto_increment,
                    feishu_token varchar(120) not null comment 'feishu file/doc token',
                    title varchar(300) not null comment 'document title',
                    content longtext null comment 'document full content (HTML/plain text)',
                    summary varchar(800) null comment 'AI generated summary',
                    tags varchar(800) null comment 'JSON array of tag strings',
                    feishu_url varchar(600) null comment 'feishu document url',
                    feishu_type varchar(40) not null default 'docx' comment 'docx/doc/sheet/bitable/folder/file/pdf',
                    feishu_modified_at bigint not null default 0 comment 'Feishu modified_time unix timestamp for change detection',
                    created_at datetime not null default current_timestamp,
                    updated_at datetime not null default current_timestamp on update current_timestamp,
                    unique key uk_feishu_token (feishu_token),
                    key idx_knowledge_doc_title (title(100)),
                    key idx_knowledge_doc_updated (updated_at desc)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci comment='Knowledge documents synced from Feishu'
                """);
        // Migrate: add column if missing
        migrateFeishuModifiedAt();
    }

    private void migrateFeishuModifiedAt() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(1) from information_schema.columns
                where table_schema = database() and table_name = 'knowledge_doc' and column_name = 'feishu_modified_at'
                """, Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.execute("alter table knowledge_doc add column feishu_modified_at bigint not null default 0");
        }
    }

    public List<KnowledgeDocEntity> search(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword + "%";
            return jdbcTemplate.query("""
                    select id, feishu_token, title, content, summary, tags,
                           feishu_url, feishu_type, feishu_modified_at, created_at, updated_at
                    from knowledge_doc
                    where title like ? or summary like ?
                    order by updated_at desc
                    limit 100
                    """, rowMapper(), like, like);
        }
        return jdbcTemplate.query("""
                select id, feishu_token, title, content, summary, tags,
                       feishu_url, feishu_type, feishu_modified_at, created_at, updated_at
                from knowledge_doc
                order by updated_at desc
                limit 100
                """, rowMapper());
    }

    public Optional<KnowledgeDocEntity> findById(Long id) {
        List<KnowledgeDocEntity> docs = jdbcTemplate.query("""
                select id, feishu_token, title, content, summary, tags,
                       feishu_url, feishu_type, feishu_modified_at, created_at, updated_at
                from knowledge_doc
                where id = ?
                """, rowMapper(), id);
        return docs.stream().findFirst();
    }

    public Optional<KnowledgeDocEntity> findByFeishuToken(String feishuToken) {
        List<KnowledgeDocEntity> docs = jdbcTemplate.query("""
                select id, feishu_token, title, content, summary, tags,
                       feishu_url, feishu_type, feishu_modified_at, created_at, updated_at
                from knowledge_doc
                where feishu_token = ?
                """, rowMapper(), feishuToken);
        return docs.stream().findFirst();
    }

    /** Update title/content/summary/feishu_modified_at for changed docs. Tags preserved. Returns the up-to-date entity. */
    public KnowledgeDocEntity upsertChanged(KnowledgeDocEntity doc) {
        long id = IdGenerator.nextId() + System.nanoTime() % 100000;
        try {
            jdbcTemplate.update("""
                    insert into knowledge_doc(id, feishu_token, title, content, summary, tags,
                        feishu_url, feishu_type, feishu_modified_at)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, doc.getFeishuToken(), doc.getTitle(), doc.getContent(),
                    doc.getSummary(), doc.getTags(), doc.getFeishuUrl(), doc.getFeishuType(),
                    doc.getFeishuModifiedAt());
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // already exists — update below
        }
        jdbcTemplate.update("""
                update knowledge_doc
                set title = ?, content = ?, summary = ?,
                    feishu_url = ?, feishu_type = ?, feishu_modified_at = ?,
                    updated_at = current_timestamp
                where feishu_token = ?
                """, doc.getTitle(), doc.getContent(), doc.getSummary(),
                doc.getFeishuUrl(), doc.getFeishuType(), doc.getFeishuModifiedAt(),
                doc.getFeishuToken());
        return findByFeishuToken(doc.getFeishuToken()).orElse(doc);
    }

    /** Insert only (for new docs), no update. Returns the entity with generated ID. */
    public KnowledgeDocEntity insertIfAbsent(KnowledgeDocEntity doc) {
        long id = IdGenerator.nextId() + System.nanoTime() % 100000;
        try {
            jdbcTemplate.update("""
                    insert into knowledge_doc(id, feishu_token, title, content, summary, tags,
                        feishu_url, feishu_type, feishu_modified_at)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, doc.getFeishuToken(), doc.getTitle(), doc.getContent(),
                    doc.getSummary(), doc.getTags(), doc.getFeishuUrl(), doc.getFeishuType(),
                    doc.getFeishuModifiedAt());
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // ok
        }
        return findByFeishuToken(doc.getFeishuToken()).orElse(doc);
    }

    public void updateTags(Long id, String tags) {
        jdbcTemplate.update("""
                update knowledge_doc
                set tags = ?, updated_at = current_timestamp
                where id = ?
                """, tags, id);
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("select count(1) from knowledge_doc", Long.class);
        return count == null ? 0 : count;
    }

    private RowMapper<KnowledgeDocEntity> rowMapper() {
        return (rs, rowNum) -> {
            KnowledgeDocEntity e = new KnowledgeDocEntity();
            e.setId(rs.getLong("id"));
            e.setFeishuToken(rs.getString("feishu_token"));
            e.setTitle(rs.getString("title"));
            e.setContent(rs.getString("content"));
            e.setSummary(rs.getString("summary"));
            e.setTags(rs.getString("tags"));
            e.setFeishuUrl(rs.getString("feishu_url"));
            e.setFeishuType(rs.getString("feishu_type"));
            e.setFeishuModifiedAt(rs.getLong("feishu_modified_at"));
            Timestamp ca = rs.getTimestamp("created_at");
            if (ca != null) e.setCreatedAt(ca.toLocalDateTime());
            Timestamp ua = rs.getTimestamp("updated_at");
            if (ua != null) e.setUpdatedAt(ua.toLocalDateTime());
            return e;
        };
    }
}

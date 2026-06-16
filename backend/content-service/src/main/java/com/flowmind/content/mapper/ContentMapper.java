package com.flowmind.content.mapper;

import com.flowmind.common.core.IdGenerator;
import com.flowmind.content.dto.CopyDraftUpdateRequest;
import com.flowmind.content.dto.CopyImageCreateRequest;
import com.flowmind.content.entity.ContentCopyEntity;
import com.flowmind.content.entity.ContentImageEntity;
import com.flowmind.content.entity.ContentThemeEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ContentMapper {
    private final JdbcTemplate jdbcTemplate;

    public ContentMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countThemes() {
        Long count = jdbcTemplate.queryForObject("select count(1) from content_theme where deleted = 0", Long.class);
        return count == null ? 0 : count;
    }

    public List<ContentThemeEntity> findThemes(String keyword, String status, String channel) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                select id, title, topic, platform, type, status, heat, rating, planned_date, summary,
                       source_type, external_ref, created_at, updated_at, deleted
                from content_theme
                where deleted = 0
                """);

        if (hasText(keyword)) {
            sql.append(" and (title like ? or topic like ? or summary like ?) ");
            String like = "%" + keyword + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (hasText(status)) {
            sql.append(" and status = ? ");
            args.add(status);
        }
        if (hasText(channel)) {
            sql.append(" and platform = ? ");
            args.add(channel);
        }
        sql.append(" order by heat desc, planned_date asc, id asc");

        return jdbcTemplate.query(sql.toString(), themeRowMapper(), args.toArray());
    }

    public Optional<ContentThemeEntity> findThemeById(Long id) {
        List<ContentThemeEntity> themes = jdbcTemplate.query("""
                select id, title, topic, platform, type, status, heat, rating, planned_date, summary,
                       source_type, external_ref, created_at, updated_at, deleted
                from content_theme
                where deleted = 0 and id = ?
                """, themeRowMapper(), id);
        return themes.stream().findFirst();
    }

    public List<String> findTagsByThemeId(Long themeId) {
        return jdbcTemplate.query("""
                select t.name
                from content_tag t
                join content_theme_tag r on r.tag_id = t.id and r.deleted = 0
                where t.deleted = 0 and r.theme_id = ?
                order by t.id
                """, (rs, rowNum) -> rs.getString("name"), themeId);
    }

    public List<ContentCopyEntity> findCopiesByThemeId(Long themeId) {
        return jdbcTemplate.query("""
                select id, theme_id, title, channel, version, style, content, usage_status,
                       used_date, generated_at, owner, feedback, rating, image_suggestion,
                       generation_source, prompt_snapshot, llm_trace_id, created_at, updated_at, deleted
                from content_copy
                where deleted = 0 and theme_id = ?
                order by generated_at desc, id desc
                """, copyRowMapper(), themeId);
    }

    public List<ContentCopyEntity> findCopies(String keyword, String channel, String usageStatus) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                select id, theme_id, title, channel, version, style, content, usage_status,
                       used_date, generated_at, owner, feedback, rating, image_suggestion,
                       generation_source, prompt_snapshot, llm_trace_id, created_at, updated_at, deleted
                from content_copy
                where deleted = 0
                """);
        if (hasText(keyword)) {
            sql.append(" and (title like ? or content like ? or image_suggestion like ?) ");
            String like = "%" + keyword + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (hasText(channel)) {
            sql.append(" and channel = ? ");
            args.add(channel);
        }
        if (hasText(usageStatus)) {
            sql.append(" and usage_status = ? ");
            args.add(usageStatus);
        }
        sql.append(" order by generated_at desc, id desc");
        return jdbcTemplate.query(sql.toString(), copyRowMapper(), args.toArray());
    }

    public Optional<ContentCopyEntity> findCopyById(Long id) {
        List<ContentCopyEntity> copies = jdbcTemplate.query("""
                select id, theme_id, title, channel, version, style, content, usage_status,
                       used_date, generated_at, owner, feedback, rating, image_suggestion,
                       generation_source, prompt_snapshot, llm_trace_id, created_at, updated_at, deleted
                from content_copy
                where deleted = 0 and id = ?
                """, copyRowMapper(), id);
        return copies.stream().findFirst();
    }

    public List<ContentImageEntity> findImagesByCopyId(Long copyId) {
        return jdbcTemplate.query("""
                select id, copy_id, file_name, url, storage_provider, object_key, sort_order,
                       created_at, updated_at, deleted
                from content_copy_image
                where deleted = 0 and copy_id = ?
                order by sort_order asc, id asc
                """, imageRowMapper(), copyId);
    }

    public void updateCopy(Long id, CopyDraftUpdateRequest request) {
        ContentCopyEntity current = findCopyById(id)
                .orElseThrow(() -> new IllegalArgumentException("文案不存在：" + id));
        jdbcTemplate.update("""
                update content_copy
                set title = ?, content = ?, usage_status = ?, used_date = ?,
                    feedback = ?, image_suggestion = ?, updated_at = current_timestamp
                where id = ? and deleted = 0
                """,
                fallback(request.title(), current.getTitle()),
                fallback(request.content(), current.getContent()),
                fallback(request.usageStatus(), current.getUsageStatus()),
                request.usedDate() == null ? current.getUsedDate() : request.usedDate(),
                fallback(request.feedback(), current.getFeedback()),
                fallback(request.imageSuggestion(), current.getImageSuggestion()),
                id
        );

        if (request.usedDate() != null) {
            jdbcTemplate.update("""
                    update content_calendar
                    set publish_date = ?, usage_status = ?, updated_at = current_timestamp
                    where copy_id = ? and deleted = 0
                    """, request.usedDate(), fallback(request.usageStatus(), current.getUsageStatus()), id);
        }
    }

    public ContentImageEntity insertImage(Long copyId, CopyImageCreateRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    insert into content_copy_image(copy_id, file_name, url, storage_provider, object_key, sort_order)
                    values (?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, copyId);
            ps.setString(2, fallback(request.name(), "uploaded-image"));
            ps.setString(3, request.url());
            ps.setString(4, fallback(request.storageProvider(), "local"));
            ps.setString(5, fallback(request.objectKey(), ""));
            ps.setInt(6, nextImageOrder(copyId));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return findImageById(key == null ? null : key.longValue())
                .orElseThrow(() -> new IllegalStateException("图片保存失败"));
    }

    public Optional<ContentImageEntity> findImageById(Long id) {
        if (id == null) return Optional.empty();
        List<ContentImageEntity> images = jdbcTemplate.query("""
                select id, copy_id, file_name, url, storage_provider, object_key, sort_order,
                       created_at, updated_at, deleted
                from content_copy_image
                where deleted = 0 and id = ?
                """, imageRowMapper(), id);
        return images.stream().findFirst();
    }

    public List<ContentCalendarRow> findCalendar(String month) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                select c.id, c.copy_id, c.theme_id, c.publish_date, cp.title, c.channel,
                       c.publish_status, c.usage_status
                from content_calendar c
                join content_copy cp on cp.id = c.copy_id and cp.deleted = 0
                where c.deleted = 0
                """);
        if (hasText(month)) {
            sql.append(" and date_format(c.publish_date, '%Y-%m') = ? ");
            args.add(month);
        }
        sql.append(" order by c.publish_date asc, c.id asc");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new ContentCalendarRow(
                rs.getLong("id"),
                rs.getLong("copy_id"),
                rs.getLong("theme_id"),
                rs.getDate("publish_date").toLocalDate(),
                rs.getString("title"),
                rs.getString("channel"),
                rs.getString("publish_status"),
                rs.getString("usage_status")
        ), args.toArray());
    }

    public void updateThemeStatus(Long id, String status) {
        jdbcTemplate.update("""
                update content_theme
                set status = ?, updated_at = current_timestamp
                where id = ? and deleted = 0
                """, status, id);
    }

    public Long insertTheme(String title, String platform, String type, String status, Integer heat,
                            String plannedDate, String summary, String topic, List<String> tags) {
        long id = IdGenerator.nextId();
        jdbcTemplate.update("""
                insert into content_theme(id, title, topic, platform, type, status, heat, rating, planned_date, summary, source_type)
                values (?, ?, ?, ?, ?, ?, ?, 0, ?, ?, 'manual')
                """, id, title, fallback(topic, title), platform, type, status, heat,
                parseDate(plannedDate), summary);
        if (tags != null && !tags.isEmpty()) {
            insertThemeTags(id, tags);
        }
        return id;
    }

    public void softDeleteTheme(Long id) {
        jdbcTemplate.update("update content_theme set deleted = 1, updated_at = current_timestamp where id = ? and deleted = 0", id);
        jdbcTemplate.update("update content_copy set deleted = 1, updated_at = current_timestamp where theme_id = ? and deleted = 0", id);
        jdbcTemplate.update("update content_calendar set deleted = 1, updated_at = current_timestamp where theme_id = ? and deleted = 0", id);
    }

    public Long insertCopy(Long themeId, String title, String channel, String version, String style,
                           String content, String owner) {
        long id = IdGenerator.nextId();
        jdbcTemplate.update("""
                insert into content_copy(id, theme_id, title, channel, version, style, content,
                       usage_status, rating, generated_at, owner, feedback, image_suggestion, generation_source)
                values (?, ?, ?, ?, ?, ?, ?, '未使用', 0, current_timestamp, ?, '', '建议使用真实咨询场景、材料截图或清单长图作为配图。', 'manual')
                """, id, themeId, title, channel, version, style, content, owner);
        return id;
    }

    public void softDeleteCopy(Long id) {
        jdbcTemplate.update("update content_copy set deleted = 1, updated_at = current_timestamp where id = ? and deleted = 0", id);
        jdbcTemplate.update("update content_calendar set deleted = 1, updated_at = current_timestamp where copy_id = ? and deleted = 0", id);
    }

    public void updateThemeRating(Long id, Integer rating) {
        jdbcTemplate.update("update content_theme set rating = ?, updated_at = current_timestamp where id = ? and deleted = 0", rating, id);
    }

    public void updateCopyRating(Long id, Integer rating) {
        jdbcTemplate.update("update content_copy set rating = ?, updated_at = current_timestamp where id = ? and deleted = 0", rating, id);
    }

    public Long insertGeneratedTheme(String title, String platform, String type, String status, Integer heat) {
        long id = IdGenerator.nextId();
        jdbcTemplate.update("""
                insert into content_theme(id, title, topic, platform, type, status, heat, rating, planned_date, summary, source_type)
                values (?, ?, ?, ?, ?, ?, ?, 0, current_date, ?, 'agent')
                """, id, title, title, platform, type, status, heat,
                "由内容智能体生成的候选主题，等待运营补充文案和排期。");
        return id;
    }

    private void insertThemeTags(Long themeId, List<String> tags) {
        for (String tagName : tags) {
            if (!hasText(tagName)) continue;
            Long tagId = findOrCreateTag(tagName.trim());
            jdbcTemplate.update("insert ignore into content_theme_tag(theme_id, tag_id) values (?, ?)", themeId, tagId);
        }
    }

    private Long findOrCreateTag(String name) {
        List<Long> ids = jdbcTemplate.query(
                "select id from content_tag where name = ? and deleted = 0",
                (rs, rowNum) -> rs.getLong("id"), name);
        if (!ids.isEmpty()) return ids.get(0);
        long newId = IdGenerator.nextId();
        jdbcTemplate.update("insert into content_tag(id, name, category) values (?, ?, 'topic')", newId, name);
        return newId;
    }

    private int nextImageOrder(Long copyId) {
        Integer max = jdbcTemplate.queryForObject(
                "select coalesce(max(sort_order), 0) from content_copy_image where copy_id = ? and deleted = 0",
                Integer.class,
                copyId
        );
        return (max == null ? 0 : max) + 1;
    }

    private RowMapper<ContentThemeEntity> themeRowMapper() {
        return (rs, rowNum) -> {
            ContentThemeEntity entity = new ContentThemeEntity();
            entity.setId(rs.getLong("id"));
            entity.setTitle(rs.getString("title"));
            entity.setTopic(rs.getString("topic"));
            entity.setPlatform(rs.getString("platform"));
            entity.setType(rs.getString("type"));
            entity.setStatus(rs.getString("status"));
            entity.setHeat(rs.getInt("heat"));
            entity.setRating(rs.getObject("rating", Integer.class));
            Date plannedDate = rs.getDate("planned_date");
            entity.setPlannedDate(plannedDate == null ? null : plannedDate.toLocalDate());
            entity.setSummary(rs.getString("summary"));
            entity.setSourceType(rs.getString("source_type"));
            entity.setExternalRef(rs.getString("external_ref"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            entity.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            entity.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
            entity.setDeleted(rs.getBoolean("deleted"));
            return entity;
        };
    }

    private RowMapper<ContentCopyEntity> copyRowMapper() {
        return (rs, rowNum) -> {
            ContentCopyEntity entity = new ContentCopyEntity();
            entity.setId(rs.getLong("id"));
            entity.setThemeId(rs.getLong("theme_id"));
            entity.setTitle(rs.getString("title"));
            entity.setChannel(rs.getString("channel"));
            entity.setVersion(rs.getString("version"));
            entity.setStyle(rs.getString("style"));
            entity.setContent(rs.getString("content"));
            entity.setUsageStatus(rs.getString("usage_status"));
            Date usedDate = rs.getDate("used_date");
            entity.setUsedDate(usedDate == null ? null : usedDate.toLocalDate());
            Timestamp generatedAt = rs.getTimestamp("generated_at");
            entity.setGeneratedAt(generatedAt == null ? null : generatedAt.toLocalDateTime());
            entity.setOwner(rs.getString("owner"));
            entity.setFeedback(rs.getString("feedback"));
            entity.setRating(rs.getObject("rating", Integer.class));
            entity.setImageSuggestion(rs.getString("image_suggestion"));
            entity.setGenerationSource(rs.getString("generation_source"));
            entity.setPromptSnapshot(rs.getString("prompt_snapshot"));
            entity.setLlmTraceId(rs.getString("llm_trace_id"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            entity.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            entity.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
            entity.setDeleted(rs.getBoolean("deleted"));
            return entity;
        };
    }

    private RowMapper<ContentImageEntity> imageRowMapper() {
        return (rs, rowNum) -> {
            ContentImageEntity entity = new ContentImageEntity();
            entity.setId(rs.getLong("id"));
            entity.setCopyId(rs.getLong("copy_id"));
            entity.setFileName(rs.getString("file_name"));
            entity.setUrl(rs.getString("url"));
            entity.setStorageProvider(rs.getString("storage_provider"));
            entity.setObjectKey(rs.getString("object_key"));
            entity.setSortOrder(rs.getInt("sort_order"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            entity.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            entity.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
            entity.setDeleted(rs.getBoolean("deleted"));
            return entity;
        };
    }

    private Date parseDate(String value) {
        return hasText(value) ? Date.valueOf(value) : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String fallback(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    public record ContentCalendarRow(
            Long id,
            Long copyId,
            Long themeId,
            LocalDate publishDate,
            String title,
            String channel,
            String publishStatus,
            String usageStatus
    ) {
    }
}

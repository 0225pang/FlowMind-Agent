package com.flowmind.content.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmind.content.dto.ContentGenerateResponse;
import com.flowmind.content.port.LocalContentRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Primary
@Component
public class MySqlLocalContentRepository implements LocalContentRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public MySqlLocalContentRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(ContentGenerateResponse response) {
        jdbcTemplate.update("""
                insert into content_generation_record(agent_type, input_json, output_json, status,
                  feishu_doc_token, feishu_bitable_record_id, vector_record_id)
                values (?, ?, ?, 'success', ?, ?, ?)
                """,
                response.getAgentType(),
                "{}",
                toJson(response),
                valueFromPersistence(response, "feishuDoc"),
                valueFromPersistence(response, "feishuBitable"),
                valueFromPersistence(response, "vectorDatabase")
        );
    }

    private String toJson(ContentGenerateResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String valueFromPersistence(ContentGenerateResponse response, String key) {
        if (response.getPersistence() == null) return null;
        Object value = response.getPersistence().get(key);
        return value == null ? null : String.valueOf(value);
    }
}
